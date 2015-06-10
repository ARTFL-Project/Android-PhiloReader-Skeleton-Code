package com.ShakespeareReaderNew.app;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


        ///////////////////////////////////////////////////
        ///////////////////////////////////////////////////
        // NOTE: this code merely demonstrates the basic //
        // way we constructed the PhiloReader apps using //
        // a drawer for search fields and fragments for  //
        // successive display pages. It should not be    //
        // considered functional. If you want to see or  //
        // learn more, please contact the ARTFL Project. // 
        ///////////////////////////////////////////////////
        ///////////////////////////////////////////////////

public class ListResultFragment extends Fragment {
    PassQueryUri makeMyQueryUri;
    BuildFullTextFragmentNew buildFullTextFragmentNew;
    BuildTOCFrag buildTOCFragment;
    QuickLinkBibFragment quickLinkBibFragment;
    ProgressDialog dialog;
    Dialog no_res_dialog;
    public Context context;

    public interface PassQueryUri {
        public void makeMyQueryUri(String my_start_hit, String my_end_hit, String spinner_value);
    }

    public interface BuildFullTextFragmentNew {
        public void buildFullTextFragmentNew(String url);
    }

    public interface BuildTOCFrag {
        public void buildTOCFragment(String[] pid_toc_query_array);
    }

    public interface QuickLinkBibFragment {
        public void quickLinkBibFragment(String ql_bib_url);
    }

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        context = getActivity().getApplicationContext();
        try {
            makeMyQueryUri = (PassQueryUri) activity;
            buildFullTextFragmentNew = (BuildFullTextFragmentNew) activity;
            buildTOCFragment = (BuildTOCFrag) activity;
            quickLinkBibFragment = (QuickLinkBibFragment) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement interface correctly");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View my_view = inflater.inflate(R.layout.list_result_linear, container, false);
        return my_view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        Activity back_activity = this.getActivity();
        Bundle bundle = this.getArguments();
        String query_uri = bundle.getString("query_uri");
        new ListResults().execute(query_uri);
    }

    private class ListResults extends AsyncTask<String, Void, ArrayList> {

	// AsyncTask sends query and gets results back //

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (dialog == null){
                dialog = new ProgressDialog(getActivity());
                dialog.setMessage("Retrieving results.");
                dialog.show();
            }
        } // end onPreExecute

        @Override
        protected ArrayList doInBackground(String... urls) {
            BufferedReader reader = null;
            ArrayList<String> all_results = new ArrayList<String>();
            try {
                String search_URI = urls[0];

                // test for kind of search //
                if (search_URI.contains("report=")){
                    report_search = true;
                    spinner_value = "concordance";
                    if (search_URI.contains("report=concordance")){
                        Log.i(TAG, "We have concordance!");
                        conc_report = true;
                    }
                    else if (search_URI.contains("report=bibliography")){
                        Pattern who_p = Pattern.compile("who=([^&]+)&start");
                        Matcher who_match = who_p.matcher(search_URI);
                        if (who_match.find()) {
                            who = true;
                        }
                        bibliography_report = true;
                    }
                }

                else {
                    report_search = false;
                    if (search_URI.contains("landing_page_content.py")) {
                        quick_link_search = true;
                    }
                }

                URI search_query = new URI(urls[0]);
                DefaultHttpClient httpclient = new DefaultHttpClient();
                HttpGet httpget = new HttpGet(search_query);

                HttpResponse response = httpclient.execute(httpget);
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();

                // read results into buffer //

                try {
                    reader = new BufferedReader(new InputStreamReader(content));
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        if (report_search) {

			    ////////////////////////////////////////////////////
			    // This here is where we parse the packaged query //
			    // results from the Philo4 database on the remote //  
			    // server. API in action. Not rocket science.     //
			    // Handling varieties of display requiring a      //
			    // list: concordance report, bibliographical      //
			    // searches, "quick links"...                     // 
			    ////////////////////////////////////////////////////

                            JSONObject jsonObject = new JSONObject(line);
                            JSONArray jsonArray = jsonObject.getJSONArray("results");
                            JSONObject query_jsonObject = jsonObject.getJSONObject("query");
                            start_hit = query_jsonObject.getInt("start");
                            total_hits = jsonObject.getInt("results_length");

                            if (conc_report != null){
                                for (int i = 0; i< jsonArray.length(); i++){
                                    JSONObject result_line = jsonArray.getJSONObject(i);
                                    text = result_line.getString("context");
                                    philoid = result_line.getString("philo_id");
                                    offsets = result_line.getString("bytes");
                                    JSONObject fulltext_link_jsonObject = result_line.getJSONObject("citation_links");
                                    String fulltext_line = fulltext_link_jsonObject.getString("div2");
                                    fulltext_line = fulltext_line.replace("http://pantagruel.ci.uchicago.edu/philologic4/shakespeare/", "");
                                    fulltext_line = fulltext_line + "&format=json";
                                    JSONObject cit_jsonObject = result_line.getJSONObject("citation");
                                    JSONObject title_jsonObject = cit_jsonObject.getJSONObject("title");
                                    title = title_jsonObject.getString("label");
                                    JSONObject date_jsonObject = cit_jsonObject.getJSONObject("date");
                                    date = date_jsonObject.getString("label");
				    // a little futzing with results output for reasons of historical compatibility //
                                    print_cit = title + " [" + date + "]";
                                    hit_number = i + start_hit;
                                    String out_pair = "<pid>" + fulltext_line + "</pid><hit>" + hit_number + "</hit>) " + print_cit + "<cmc>" + text;
                                    all_results.add(out_pair);
                                }
                            }

                            else if (bibliography_report != null){
                                if (who != null){
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        JSONObject result_line = jsonArray.getJSONObject(i);
                                        philoid = result_line.getString("philo_id");
                                        JSONObject cit_jsonObject = result_line.getJSONObject("citation");
                                        JSONObject speaker_jsonObject = cit_jsonObject.getJSONObject("para");
                                        speaker = speaker_jsonObject.getString("label");
                                        speaker_link = speaker_jsonObject.getString("href");
                                        speaker_link = speaker_link.replace("http://pantagruel.ci.uchicago.edu/philologic4/shakespeare/navigate/", "");
                                        speaker_link = speaker_link.replaceAll("/", "%20");
                                        speaker_link = "/reports/navigation.py?report=navigate&philo_id=" + speaker_link;
                                        JSONObject line_jsonObject = cit_jsonObject.getJSONObject("page");
                                        String line_no = line_jsonObject.getString("label");
                                        line_no = line_no.replace("page", "line");
                                        JSONObject scene_jsonObject = cit_jsonObject.getJSONObject("div2");
                                        String act_scene = scene_jsonObject.getString("label");
                                        JSONObject meta_jsonObject = result_line.getJSONObject("metadata_fields");
                                        title = meta_jsonObject.getString("title");
                                        date = meta_jsonObject.getString("date");

                                        print_cit = title + " [<b>" + date + "</b>]";
                                        hit_number = i + start_hit;
                                        String out_pair = "<pid>" + speaker_link + "</pid><hit>" + hit_number + "</hit>) " + print_cit + "<cmc>Speaker: " + speaker +
                                                " <i>" + act_scene + " " + line_no + "</i>";
                                        all_results.add(out_pair);
                                    }
                                }
                                else {
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        JSONObject result_line = jsonArray.getJSONObject(i);
                                        philoid = result_line.getString("philo_id");
                                        JSONObject cit_jsonObject = result_line.getJSONObject("citation");
                                        JSONObject title_jsonObject = cit_jsonObject.getJSONObject("title");
                                        title = title_jsonObject.getString("label");
                                        JSONObject date_jsonObject = cit_jsonObject.getJSONObject("date");
                                        date = date_jsonObject.getString("label");

                                        print_cit = title + " [<b>" + date + "</b>]";
                                        hit_number = i + 1;
                                        String out_pair = "<pid>" + philoid + "</pid><hit><cmc>" + hit_number + "</hit>) " + print_cit;
                                        all_results.add(out_pair);
                                    }
                                }
                            }

                        }
                        else {
                            if (quick_link_search != null) {
                                JSONArray jsonArray = new JSONArray(line);
                                total_hits = jsonArray.length();
                                for (int i = 0; i< jsonArray.length(); i++){
                                    JSONObject result_line = jsonArray.getJSONObject(i);
                                    cite = result_line.getString("title");
                                    quick_link_link = result_line.getString("url");
                                    hit_number = i + 1;
                                    cite = cite.replace("<a href", "<a link");
                                    String out_pair = "<hit>" + hit_number + "</hit>) <a link=\"" +  quick_link_link + "\">" + cite;
                                    all_results.add(out_pair);
                                }
                            }
                        }
                    }
                }
                catch (IOException exception) {
                    Log.e(TAG, "Here? IOException --> " + exception.toString());
                }
                // pro-forma cleanup //
                finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        }
                        catch (IOException exception) {
                            Log.e(TAG, "IOException --> " + exception.toString());
                        }
                    }
                }
            }
            // Exception for problems with HTTP connection //
            catch (Exception exception) {
                Log.e(TAG, "Trouble connecting -->" + exception.toString());
                return null;
            }
            return all_results;
        } // end doInBackGround

        @Override
        protected void onPostExecute(ArrayList all_results) {
            if (dialog != null){
                dialog.dismiss();
            }

            final Boolean bibliography_report2pass = bibliography_report; // need this for 'inner class'

            final TextView mTextView;
            final ListView mListView;
            final String count_display;

            if (getView() == null) {
                View view = LayoutInflater.from(context).inflate(R.layout.list_result_linear, null);
                Log.i(TAG, " View was null: " + view.toString());
                mTextView = (TextView) view.findViewById(R.id.hit_count);
                mListView = (ListView) view.findViewById(R.id.results_list);
            } else {
                Log.i(TAG, " The View from here: " + getView().toString());
                mTextView = (TextView) getView().findViewById(R.id.hit_count);
                mListView = (ListView) getView().findViewById(R.id.results_list);
            }

            count_display = context.getResources().getQuantityString(R.plurals.search_results_count, total_hits, total_hits);

            mTextView.setText(count_display);
            if (all_results != null && !all_results.isEmpty()) {

		///////////////////////////////////////////////////
		// Here we process clicks on list items and send //
		// philo ids or other data back to MainActivity  //
                // to execute new searchs/spawn new fragments.   //
		///////////////////////////////////////////////////

                if (quick_link_search != null){
                    try {
                        DisplayQuicklinkAdapter linkAdapter = new DisplayQuicklinkAdapter(context, R.layout.result, all_results);
                        mListView.setAdapter(linkAdapter);
                    }
                    catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                        @Override

                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            String one_more_click_link = mListView.getItemAtPosition(position).toString();
                            Pattern pid_regex = Pattern.compile("navigate/([^/]*)/table-of-contents");
                            Matcher pid_match = pid_regex.matcher(one_more_click_link);
                            if (pid_match.find()){
                                String ql_bib_url = pid_match.group(1);
                                quickLinkBibFragment.quickLinkBibFragment(ql_bib_url);
                            }
                        }
                    });

                } // end quick link handling

                else { // now handle standard conc and bib results
                    try {
                        DisplayResultsAdapter outAdapter = new DisplayResultsAdapter(context, R.layout.result, all_results);
                        mListView.setAdapter(outAdapter);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    if (total_hits > 25 && who != null || conc_report != null) {
                        int next_start_hit = start_hit + 25;
                        int next_end_hit = next_start_hit + 24;

                        final String my_start_hit = Integer.toString(next_start_hit);
                        final String my_end_hit = Integer.toString(next_end_hit);
                        final String my_prev_start_hit = Integer.toString(start_hit - 25);
                        final String my_prev_end_hit = Integer.toString(start_hit - 1);

                        View buttons_view = LayoutInflater.from(context).inflate(R.layout.image_buttons, null);

                        ImageButton prev_btn = (ImageButton) buttons_view.findViewById(R.id.ll_previous);
                        ImageButton next_btn = (ImageButton) buttons_view.findViewById(R.id.ll_next);

                        next_btn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                makeMyQueryUri.makeMyQueryUri(my_start_hit, my_end_hit, spinner_value);
                            }
                        });

                        prev_btn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                makeMyQueryUri.makeMyQueryUri(my_prev_start_hit, my_prev_end_hit, spinner_value);
                            }
                        });

                        if (start_hit == 1) {
                            prev_btn.setAlpha(chuck_float);
                            prev_btn.setOnClickListener(null);
                        }
                        else if (total_hits < next_start_hit) {
                            next_btn.setAlpha(chuck_float);
                            next_btn.setOnClickListener(null);
                        }

                        final View insertPoint;
                        if (getView() == null){
                            View view = LayoutInflater.from(context).inflate(R.layout.list_result_linear, null);
                            insertPoint = view.findViewById(R.id.list_res_linear);
                            }
                        else {
                            insertPoint = getView().findViewById(R.id.list_res_linear);

                        }
                        ((ViewGroup) insertPoint).addView(buttons_view, 1, new
                                ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    } // end next + prev button code

                    mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                        @Override

                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            String single_result_hit = mListView.getItemAtPosition(position).toString();
                            String pid_query_string_match = "";
                            Pattern pid_regex = Pattern.compile("<pid>([^<]*)</pid>");
                            Matcher pid_match = pid_regex.matcher(single_result_hit);

                            if (bibliography_report2pass == null){
                                if (pid_match.find()) {
                                    String url = pid_match.group(1);
                                    buildFullTextFragmentNew.buildFullTextFragmentNew(url);
                                }
                            }
                            else {
                                if (pid_match.find()){
                                    if (who != null){
                                        String url = pid_match.group(1);
                                        buildFullTextFragmentNew.buildFullTextFragmentNew(url);
                                    }
                                    else {
                                        pid_query_string_match = pid_match.group(1);
                                        String[] pid_query_array = pid_query_string_match.split(",");
                                        buildTOCFragment.buildTOCFragment(pid_query_array);
                                    }
                                }
                            }
                        }
                    }); // end click listener
                } // end of bib and conc result handling

            } // end of code handling queries with results

            else { // Generate no results message in button //
               final Dialog dialog = new Dialog(getActivity());
               dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
               dialog.setContentView(R.layout.no_results_dialog);
               dialog.getWindow().getAttributes().dimAmount = 0;
               dialog.setCanceledOnTouchOutside(true);
               dialog.show();
            }

        } // end onPostExecute

    } // end AsyncTask

} // last and final...
