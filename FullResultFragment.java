package com.ShakespeareReaderNew.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
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


public class FullResultFragment extends Fragment {
    String TAG = "FullResultFragment";
    BuildFullTextFragmentNew buildFullTextFragmentNew;
    BuildTOCFrag buildTOCFragment;
    PassBookmarkGoodies passBookmarkGoodies;
    ProgressDialog dialog;
    public WebView mWebView;
    public TextView mTextView;
    public String bookmarkPhiloId2Send = "";
    Context context;


    public interface BuildFullTextFragmentNew {
        public void buildFullTextFragmentNew(String url);
    }

    public interface BuildTOCFrag {
        public void buildTOCFragment(String[] pid_toc_query_array);
    }

    public interface PassBookmarkGoodies {
        public void passBookmarkGoodies(String full_shrtcit, boolean addBookmarkBoolean, String bookmarkPhiloId2Send);
    }

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        context = getActivity().getApplicationContext();
        try {
            buildFullTextFragmentNew = (BuildFullTextFragmentNew) activity;
            buildTOCFragment = (BuildTOCFrag) activity;
            passBookmarkGoodies = (PassBookmarkGoodies) activity;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " Problem with asyncResponse");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.full_result_linear, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        Bundle bundle = this.getArguments();
        String new_query_uri = bundle.getString("query_uri");
        String test_activity = this.getActivity().toString();
        new FullResults().execute(new_query_uri);

    }

    private class FullResults extends AsyncTask<String, Void, ArrayList>{

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

                           ///////////////////////////////////////////////////////
                            // This here is where we parse the packaged query   //
                            // results from the Philo4 database on the remote   //  
                            // server. API in action. Still not rocket science. //
                            // Handling only fulltext chunks here.              //
                            //////////////////////////////////////////////////////

                    while ((line = reader.readLine()) != null) {
                        JSONObject jsonObject = new JSONObject(line);
                        String json_string = jsonObject.getString("text");
                        full_philo_id = jsonObject.getString("philo_id");
                        String[] philo_id_bits = full_philo_id.split(" ");
                        String philo_id = philo_id_bits[0];

                        prev = jsonObject.getString("prev");
                        prev = prev.replaceAll(" ", "%20");
                        prev = "/reports/navigation.py?report=navigate&philo_id=" + prev;
                        next = jsonObject.getString("next");
                        next = next.replaceAll(" ", "%20");
                        next = "/reports/navigation.py?report=navigate&philo_id=" + next;

                        JSONObject cit_jsonObject = jsonObject.getJSONObject("citation");
                        JSONObject cit_title_jsonObject = cit_jsonObject.getJSONObject("title");
                        title = cit_title_jsonObject.getString("label");
                        JSONObject cit_date_jsonObject = cit_jsonObject.getJSONObject("date");
                        String date = cit_date_jsonObject.getString("label");
                        citation = "<a data-id=\"" + philo_id + "\"><i>" + title + "</i> [<b>" + date + "</b>]</a>";
                        String section = "";

                        JSONObject cit_div_jsonObject = cit_jsonObject.getJSONObject("div2");
                        String section = cit_div_jsonObject.getString("label");
                        full_shrtcit = title + " " + section;

                        String info_tag = "<shrt>" + full_shrtcit + "</shrt><title>" + citation + "</title>";
                        String out_pair = info_tag + json_string;
                        all_results.add(out_pair);
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

	    ////////////////////////////////////////////////
	    // Just displaying fulltext objects, handling //
	    // clicks forward and back in the text, and   //
	    // getting ids for bookmarkable chunks.       //
	    ////////////////////////////////////////////////

            String results_array = all_results.toString();

            full_philo_id = full_philo_id.replaceAll("[\\[\\]]", "");
            String[] bookMarkEdit = full_philo_id.split(" ");
            String third_level_bit = ""; 
            if (bookMarkEdit.length == 2){
                third_level_bit = "0";
            }
            else {
                third_level_bit = bookMarkEdit[2];
            }
            bookmarkPhiloId2Send = bookMarkEdit[0] + "%20" + bookMarkEdit[1] + "%20" + third_level_bit;
            String[] split_results_array = results_array.split("</title>");
            String[] title_chunk = split_results_array[0].split("</shrt>");
            String title_string = title_chunk[1];
            title_string = title_string.replace("<title>", "<div class=\"title\">");
            title_string = title_string.replace("</title>", "</div>");
            title_string = title_string.replace("|", "&nbsp;");
            title_string = title_string.trim();
            String results_string = split_results_array[1];
            results_string = results_string.replaceFirst("]$","");

            ImageButton next_btn;
            ImageButton prev_btn;

            if (getView() == null){
                View view = LayoutInflater.from(context).inflate(R.layout.full_result_linear, null);
                prev_btn = (ImageButton) view.findViewById(R.id.ll_previous);
                next_btn = (ImageButton) view.findViewById(R.id.ll_next);
                }
            else {
                prev_btn = (ImageButton) getView().findViewById(R.id.ll_previous);
                next_btn = (ImageButton) getView().findViewById(R.id.ll_next);
            }

            next_btn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    buildFullTextFragmentNew.buildFullTextFragmentNew(next);
                }
            });

            prev_btn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    buildFullTextFragmentNew.buildFullTextFragmentNew(prev);
                }
            });

            if (prev.isEmpty()){
                prev_btn.setAlpha(chuck_float);
                prev_btn.setOnClickListener(null);
            }
            if (next.isEmpty()){
                next_btn.setAlpha(chuck_float);
                next_btn.setOnClickListener(null);
            }

            if (getView() == null){
                View view = LayoutInflater.from(context).inflate(R.layout.full_result_linear, null);
                mTextView = (TextView) view.findViewById(R.id.full_text_title);
                mWebView = (WebView) view.findViewById(R.id.full_wv_text_result);
                }
            else {
                mTextView = (TextView) getView().findViewById(R.id.full_text_title);
                mWebView = (WebView) getView().findViewById(R.id.full_wv_text_result);
            }

            mTextView.setText(Html.fromHtml(title_string));
            final String getTOC = title_string;
            mTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String pid_query_string_match = "";
                    Pattern pid_regex = Pattern.compile("<a data-id=\"([^\"]*)\"");
                    Matcher pid_match = pid_regex.matcher(getTOC);
                    if (pid_match.find()){
                        pid_query_string_match = pid_match.group(1);
                        String[] pid_query_array = pid_query_string_match.split(" ");
                        buildTOCFragment.buildTOCFragment(pid_query_array);
                    }
                }
            });
            String html_header = "<html><head><link href=\"philoreader.css\" type=\"text/css\" rel=\"stylesheet\">";
            html_header = html_header.concat("<script src=\"http://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js\"></script>");
            html_header = html_header.concat("<script src=\"file:///android_asset/scroll2hit.js\" type=\"text/javascript\"></script>");
            html_header = html_header.concat("<script src=\"file:///android_asset/popnote.js\" type=\"text/javascript\"></script></head>");
            results_string = "<body>" + html_header + results_string + "</body>";

            mWebView.getSettings().setBuiltInZoomControls(true);
            mWebView.getSettings().setJavaScriptEnabled(true);
            mWebView.setWebViewClient(new com.ShakespeareReaderNew.app.MyWebViewClient(){
                @Override
                public void onPageFinished(WebView view, String url){
                    view.loadUrl("javascript:getOffset();");
                }
            });
            mWebView.loadDataWithBaseURL("file:///android_asset/", results_string, "text/html", "utf-8", "");

            Boolean addBookmarkBoolean;
            addBookmarkBoolean = true;
            passBookmarkGoodies.passBookmarkGoodies(full_shrtcit, addBookmarkBoolean, bookmarkPhiloId2Send);

        } // end onPostExecute
    } // end async

} // end end


