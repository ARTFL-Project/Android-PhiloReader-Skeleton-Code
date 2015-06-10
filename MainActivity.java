package com.ShakespeareReaderNew.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.concurrent.ExecutionException;

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

public class MainActivity extends FragmentActivity implements
        ListResultFragment.PassQueryUri,
        ListResultFragment.BuildFullTextFragmentNew,
        ListResultFragment.BuildTOCFrag,
        ListResultFragment.QuickLinkBibFragment,
        FullResultFragment.BuildFullTextFragmentNew,
        FullResultFragment.BuildTOCFrag,
        FullResultFragment.PassBookmarkGoodies,
        TOCResultFragment.BuildFullTextFragmentNew,
        FreqResultFragment.BuildListFragment,
        QuickLinksFragment.QuickLinkSearch,
        InfoFragment.QuickLinkSearch {

	///////////////////////////////////////////////////////////
	// UI code and boilerplate code for basic funcationality //
	///////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Drawer settings //

        setContentView(R.layout.drawer_main);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_mainView);
        spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getApplicationContext(), R.array.report_options,
                R.layout.spinner_item);
        spinner.setAdapter(adapter);

        // ActionBar code // 

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

	// Check internet connection //

        cd = new com.ShakespeareReaderNew.app.ConnectionDetector(getApplicationContext());

        if (!cd.isConnectingToInternet()) {
            final Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.requestWindowFeature(Window.FEATURE_LEFT_ICON);
            dialog.setContentView(R.layout.no_connection_dialog);
            dialog.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_action_warning );
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.DIM_AMOUNT_CHANGED, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            dialog.getWindow().getAttributes().dimAmount = 0;

            final Button b = (Button) dialog.findViewById(R.id.no_connection_button);

            b.setOnLongClickListener(new Button.OnLongClickListener() {
                public boolean onLongClick(final View v) {
                    b.setBackgroundColor(0xffde5800);
                    dialog.dismiss();
                    System.exit(0);
                    return true;
                }
            });

            dialog.setCanceledOnTouchOutside(true);
            dialog.show();

        }
        else {

	    // Check remote server status //

            url_con = new com.ShakespeareReaderNew.app.UrlConnect();
            url_con.execute();
            try {
                if (!url_con.get()) {
                    final Dialog dialog = new Dialog(this);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.requestWindowFeature(Window.FEATURE_LEFT_ICON);
                    dialog.setContentView(R.layout.server_down_dialog);
                    dialog.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_action_warning );
                    dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.DIM_AMOUNT_CHANGED, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
                    dialog.getWindow().getAttributes().dimAmount = 0;

                    final Button b = (Button) dialog.findViewById(R.id.server_down_button);

                    b.setOnLongClickListener(new Button.OnLongClickListener() {
                        public boolean onLongClick(final View v) {
                            b.setBackgroundColor(0xffde5800);
                            dialog.dismiss();
                            System.exit(0);
                            return true;
                        }
                    });

                    dialog.setCanceledOnTouchOutside(true);
                    dialog.show();
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        addBookmark = new com.ShakespeareReaderNew.app.AddBookmark(getApplicationContext());
        addBookmark.createDataBase();

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,R.string.open_drawer, R.string.close_drawer) {
            public void onDrawerClosed(View view) {
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu();
                conc_from_freq = null;

                spinner = (Spinner) findViewById(R.id.spinner);
                spinner.setOnItemSelectedListener(new OnItemSelectedListener(){
                    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id){
                        String query_type_to_send = parent.getItemAtPosition(pos).toString();
                        spinner_value = newQuerySelector(query_type_to_send);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                        }
                    });

            } // end onDrawerOpened
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // Deal with search form submit and reset //

        Button search = (Button) findViewById(R.id.search_button);
        search_et = (EditText)findViewById(R.id.search_edittext);
        speaker_et = (EditText) findViewById(R.id.speaker_edittext);
        //author_et = (EditText) findViewById(R.id.author_edittext);
        title_et = (EditText) findViewById(R.id.title_edittext);

        search.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Context context = MainActivity.this;
                final String my_start_hit = "1";
                final String my_end_hit = "25";
                makeMyQueryUri(my_start_hit, my_end_hit, spinner_value);
                mDrawerLayout.closeDrawer(Gravity.LEFT);
                InputMethodManager imm = (InputMethodManager)getSystemService(context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(search_et.getWindowToken(), 0);

            }
        });

        Button reset = (Button) findViewById(R.id.search_reset);
        reset.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                search_et.setText("");
                speaker_et.setText("");
                title_et.setText("");
                Spinner spinner = (Spinner) findViewById(R.id.spinner);
                spinner.setSelection(0);
            }
        });

        if (findViewById(R.id.text) != null) {
            if (savedInstanceState != null) {
                return;
            }
            QuickLinksFragment quickLinksFragment = new QuickLinksFragment();
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction fragTransaction = fm.beginTransaction();
            fragTransaction.addToBackStack(null);
            fragTransaction.add(R.id.text, quickLinksFragment).commit();
        }

    }

    public String newQuerySelector(String query_selection){
        if (query_selection.contains("Concordance Report")){
            query_search_type = "concordance";
            }
        //else if (query_selection.contains("Frequency by Author")){
        //    query_search_type = "author";
        //}
        else if (query_selection.contains("Frequency by Title")){
            query_search_type = "title";
        }
        else if (query_selection.contains("Frequency by Date")){
            query_search_type = "date";
        }
        else if (query_selection.contains("Frequency by Speaker")) {
            query_search_type = "who";
        }
        return query_search_type;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft;
            ft = fm.beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
            if (fm.getBackStackEntryCount() == 1){
                mDrawerLayout.openDrawer(Gravity.LEFT);
                return false;
                }
            ft.commit();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        // show bookmarks here //
        bookmarkMenu = menu.findItem(R.id.show_bookmarks).getSubMenu();
        Cursor cursor = addBookmark.showBookmarkItems();
        if (cursor == null){
        }
        else {
            cursor.moveToFirst();
            if (cursor.moveToFirst()){
                do {
                    bookmarkMenu.add(cursor.getString(1));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        addBookmark.close();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
         super.onPostCreate(savedInstanceState);
         mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
         super.onConfigurationChanged(newConfig);
         mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()){
            case R.id.app_info:
                lookingAtInfo = true;
                return true;
            case R.id.info1:
                displayInfoDialog("about_app");
                return true;
            case R.id.info2:
                displayInfoDialog("about_artfl");
                return true;
            case R.id.info3:
                displayInfoDialog("quick_links");
                return true;
            case R.id.show_bookmarks:
                lookingAtInfo = false;
                return true;
            case R.id.bookmark_this:
                invalidateOptionsMenu();
                if (canAddBookmark){
                    if (thisIsABookmark){
                        Toast.makeText(this, "You are viewing a bookmarked page.", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        invalidateOptionsMenu();
                        bookMark();
                    }
                }
                else {
                    if (thisIsABookmark) {
                        Toast.makeText(this, "You are viewing a bookmarked page.", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(this, "This element cannot be bookmarked.", Toast.LENGTH_SHORT).show();
                    }
                }
                return true;

            default:
                if (!lookingAtInfo){
                    selected_bookmark = item.toString();
                    final String[] items = {getResources().getString(R.string.view_bookmark), getResources().getString(R.string.delete_bookmark)};
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(item.getTitle());
                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            if (items[item].equals(getResources().getString(R.string.view_bookmark)) ) {
                                String bookmark_uri = addBookmark.getBookmarkedText(selected_bookmark);
                                Bundle bundle = new Bundle();
                                bundle.putString("query_uri", bookmark_uri);
                                Fragment fr;
                                fr = new com.ShakespeareReaderNew.app.FullResultFragment();
                                fr.setArguments(bundle);
                                FragmentManager fm = getSupportFragmentManager();
                                FragmentTransaction fragTransaction = fm.beginTransaction();
                                fragTransaction.replace(R.id.text, fr, "text");
                                fragTransaction.addToBackStack(null);
                                fragTransaction.commit();
                                thisIsABookmark = true;
                                }
                            else if (items[item].equals(getResources().getString(R.string.delete_bookmark)) ) {
                                addBookmark.deleteBookmark(selected_bookmark);
                                invalidateOptionsMenu();
                                Toast.makeText(getApplicationContext(), "Deleting " + selected_bookmark, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    AlertDialog alert = builder.create();
                    alert.show();
                    addBookmark.close();
                }
            return true;
        } // end switch logic
    }

    public void passBookmarkGoodies(String full_shrtcit, boolean addBookmarkBoolean, String bookmarkPhiloId2Send) {
        bookmarkShrtCit = full_shrtcit;
        canAddBookmark = addBookmarkBoolean;
        bookmarkPhiloId = bookmarkPhiloId2Send;
    }

    public void bookMark() {
        addBookmark.open();
        addBookmark.addBookmarkItem(bookmarkPhiloId, bookmarkShrtCit);
        addBookmark.close();
        bookmarkShrtCit = "";
        canAddBookmark = false;
        bookmarkPhiloId = "";
    }

    /////////////////////////////////////////////////////////////////////
    /// Here on down are query building and navigation/fragment code. ///
    /// All pretty much similar, just creating URLs to send and get   ///
    /// results in new fragments...                                   ///
    /////////////////////////////////////////////////////////////////////

    public void makeMyQueryUri(String my_start_hit, String my_end_hit, String spinner_value) {

	//////////////////////////////////////////
	// ** Build all query uris by hand!  ** //
	// ** Uri.Builder does NOT work well ** //
	//////////////////////////////////////////

        String my_report_value = "";
        String my_query_uri = "";
        String frequency_field = "";
        if (search_et.getText().toString().isEmpty()) {
		my_report_value = "bibliography";
                String query_speaker = speaker_et.getText().toString();
                query_speaker = query_speaker.trim();
                query_speaker = query_speaker.replaceAll(" ", "+");
                String query_title = title_et.getText().toString();
                query_title = query_title.trim();
                query_title = query_title.replaceAll(" ", "+");
                query_title = query_title.replace("|", "%7C");
                my_query_uri = "http://" + uri_authority + "/" + 
		    philo_dir + "/" + 
		    build_name + "/query?" +
                    "report=bibliography&q=&method=proxy&title=" + 
		    query_title +
                    "&who=" + query_speaker +
                    "&start=" + my_start_hit + "&end=" + my_end_hit + "&pagenum=25&format=json";
        } else {
            if (conc_from_freq != null){
                my_report_value = "concordance";
                freq_search_term = freq_search_term.trim();
                freq_search_term = freq_search_term.replaceAll(" ", "+");
                freq_search_term = freq_search_term.replace("|", "%7C");
                //conc_author_from_freq = conc_author_from_freq.trim();
                //conc_author_from_freq = conc_author_from_freq.replaceAll(" ", "+");
                conc_title_from_freq = conc_title_from_freq.trim();
                conc_title_from_freq = conc_title_from_freq.replaceAll(" ", "+");
                conc_title_from_freq = conc_title_from_freq.replace("|", "%7C");
                my_query_uri = "http://" + uri_authority + "/" + 
			philo_dir + "/" + 
			build_name + "/navigate?" +
                        "report=concordance&" + freq_search_term + "&method=proxy&" +
                        conc_title_from_freq + "&" + conc_date_from_freq +
                        "&start=" + my_start_hit + "&end=" + my_end_hit + "&pagenum=25&format=json";
            }
            else if (spinner_value.contains("concordance")) {
                my_report_value = "concordance";
                String query_term = search_et.getText().toString();
                query_term = query_term.trim();
                query_term = query_term.replaceAll(" ", "+");
                query_term = query_term.replace("|", "%7C");

                String query_title = title_et.getText().toString();
                query_title = query_title.trim();
                query_title = query_title.replaceAll(" ", "+");
                query_title = query_title.replace("|", "%7C");
                String query_speaker = speaker_et.getText().toString();
                query_speaker = query_speaker.trim();
                query_speaker = query_speaker.replaceAll(" ", "+");
                my_query_uri = "http://" + uri_authority + "/" + 
			philo_dir + "/" + 
			build_name + "/navigate?" +
                        "report=concordance&q=" + query_term + "&method=proxy&title=" +
                         query_title +
                        "&who=" + query_speaker +
                        "&start=" + my_start_hit + "&end=" + my_end_hit + "&pagenum=25&format=json";
            } else {
                frequency_field = spinner_value;
                my_report_value = "frequency";
                String query_term = search_et.getText().toString();
                query_term = query_term.trim();
                query_term = query_term.replaceAll(" ", "+");
                String query_title = title_et.getText().toString();
                query_title = query_title.trim();
                query_title = query_title.replaceAll(" ", "+");
                query_title = query_title.replace("|", "%7C");
                String query_speaker = speaker_et.getText().toString();
                query_speaker = query_speaker.trim();
                query_speaker = query_speaker.replaceAll(" ", "+");
                my_query_uri = "http://" + uri_authority + "/" + 
			philo_dir + "/" + 
			build_name + "/scripts/get_frequency.py?" +
                        "report=concordance&q=" + query_term + "&method=proxy&title=" +
                        query_title +
                        "&who=" + query_speaker +
                        "&frequency_field=" + frequency_field + "&format=json";
            }
        }

        Bundle bundle = new Bundle();
        bundle.putString("query_uri", my_query_uri);
        Fragment fr;
        if (my_report_value.contains("frequency")){
            fr = new com.ShakespeareReaderNew.app.FreqResultFragment();
            }
        else {
            fr = new com.ShakespeareReaderNew.app.ListResultFragment();
        }
        fr.setArguments(bundle);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragTransaction = fm.beginTransaction();
        fragTransaction.addToBackStack(null);
        fragTransaction.replace(R.id.text, fr, "text");
        fragTransaction.commit();
    }

    public void displayInfoDialog(String info_string){
        String file_name = info_string + ".html";
        Bundle bundle = new Bundle();
        bundle.putString("file_name", file_name);
        Fragment fr;
        fr = new com.ShakespeareReaderNew.app.InfoFragment();
        fr.setArguments(bundle);
        FragmentManager fm = getSupportFragmentManager();
        Log.i(TAG, " Info click backstack count: " + fm.getBackStackEntryCount());
        FragmentTransaction fragTransaction = fm.beginTransaction();
        fragTransaction.addToBackStack(null);
        fragTransaction.replace(R.id.text, fr, "text");
        fragTransaction.commit();
        lookingAtInfo = false;
    }

    public void quickLinkSearch(String quick_link_url){
        String my_query_url = quick_link_url.replace("file://", "");
        my_query_url = "http://" + uri_authority + "/" + philo_dir + "/"+ build_name + my_query_url;
        Bundle bundle = new Bundle();
        bundle.putString("query_uri", my_query_url);
        Fragment fr;
        fr = new ListResultFragment();
        fr.setArguments(bundle);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragTransaction = fm.beginTransaction();
        fragTransaction.addToBackStack(null);
        fragTransaction.replace(R.id.text, fr,"text");
        fragTransaction.commit();
    }

    public void quickLinkBibFragment(String ql_bib_url){
        String my_query_url = "";
        if (ql_bib_url.contains("author")){
            ql_bib_url = ql_bib_url.replace("./?q=", "");
            my_query_url = "http://" + uri_authority + "/" + 
		philo_dir + "/" + 
		build_name +
                "/navigate?report=bibliography&" + ql_bib_url + "&format=json";
            Bundle bundle = new Bundle();
            bundle.putString("query_uri", my_query_url);
            Fragment fr;
            fr = new com.ShakespeareReaderNew.app.ListResultFragment();
            fr.setArguments(bundle);
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction fragTransaction = fm.beginTransaction();
            fragTransaction.addToBackStack(null);
            fragTransaction.replace(R.id.text, fr, "text");
            fragTransaction.commit();
           }
        else {
            my_query_url = "http://" + uri_authority + "/" + 
		philo_dir + "/" + 
		build_name +
		"/scripts/get_table_of_contents.py?" +
		"philo_id=" + ql_bib_url + "&format=json";
            Bundle bundle = new Bundle();
            bundle.putString("query_uri", my_query_url);
            Fragment fr;
            fr = new TOCResultFragment();
            fr.setArguments(bundle);
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction fragTransaction = fm.beginTransaction();
            fragTransaction.replace(R.id.text, fr, "text");
            fragTransaction.addToBackStack(null);
            fragTransaction.commit();
        }
    }

    public void buildListFragment(String query_uri_from_freq){
        conc_from_freq = true;
        conc_title_from_freq = "";
        conc_author_from_freq = "";
        conc_date_from_freq = "";
        freq_search_term = "";
        String query_uri_to_munge = query_uri_from_freq;
        String[] freq_query_params = query_uri_to_munge.split("&");
        for (int i = 0; i < freq_query_params.length; i++){
            Log.i(TAG, "Your freq_query_params: " + freq_query_params[i]);
            if (freq_query_params[i].contains("q=")){
                freq_search_term = freq_query_params[i];
            }
            else if (freq_query_params[i].contains("title=")){
                conc_title_from_freq = freq_query_params[i];
            }
            else if (freq_query_params[i].contains("author=")){
                conc_author_from_freq = freq_query_params[i];
            }
            else if (freq_query_params[i].contains("date=")){
                conc_date_from_freq = freq_query_params[i];
            }
        }
        Bundle bundle = new Bundle();
        bundle.putString("query_uri", query_uri_from_freq);
        Fragment fr;
        fr = new com.ShakespeareReaderNew.app.ListResultFragment();
        fr.setArguments(bundle);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragTransaction = fm.beginTransaction();
        fragTransaction.addToBackStack(null);
        fragTransaction.replace(R.id.text, fr, "text");
        fragTransaction.commit();
    }

    public void buildTOCFragment(String[] pid_toc_query_array){
        String pid_toc_address = "";
        String toc_query_uri = "";
        pid_toc_address = pid_toc_query_array[0].replaceFirst("\\[", "");

        toc_query_uri = "http://" + uri_authority + "/" + 
		philo_dir + "/" + 
		build_name +
                "/scripts/get_table_of_contents.py?" +
                "philo_id=" + pid_toc_address + "&format=json";
        Bundle bundle = new Bundle();
        bundle.putString("query_uri", toc_query_uri);
        Fragment fr;
        fr = new com.ShakespeareReaderNew.app.TOCResultFragment();
        fr.setArguments(bundle);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragTransaction = fm.beginTransaction();
        fragTransaction.replace(R.id.text, fr, "text");
        fragTransaction.addToBackStack(null);
        fragTransaction.commit();
    }

    public void buildFullTextFragmentNew(String url){
        String new_query_uri = "";
        url = url.replace("file:///", "");
        new_query_uri = "http://" + uri_authority + "/" + 
		philo_dir + "/" + 
		build_name + "/" +
                url;
        Bundle bundle = new Bundle();
        bundle.putString("query_uri", new_query_uri);
        Fragment fr;
        fr = new FullResultFragment();
        fr.setArguments(bundle);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragTransaction = fm.beginTransaction();
        fragTransaction.replace(R.id.text, fr, "text");
        fragTransaction.addToBackStack(null);
        fragTransaction.commit();
    }

} // the end, beautiful friend
