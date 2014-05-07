package com.sleam.nfc;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;

/**
 * @author chaasof@Cbeing
 * support URL: http://code.tutsplus.com/tutorials/reading-nfc-tags-with-android--mobile-17278
 */

public class MainActivity extends Activity {

	public static final String MIME_TEXT_PLAIN = "text/plain";
	public static final String TAG = "Nfc";
	private TextView mTextView;
	private NfcAdapter mNfcAdapter;
	//JSONArray etudiants;
	JSONObject rootObj;

	private Button button01;
    private TextView textView01;
	private ArrayList<JSONObject> etudiants;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        button01 = (Button)findViewById(R.id.button1);
        textView01 = (TextView)findViewById(R.id.textView1);
        
        etudiants = new ArrayList<JSONObject>();
	    rootObj = new JSONObject();
	    button01.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				JSONArray et = new JSONArray();
				Iterator it = etudiants.iterator();
				while(it.hasNext()) {
					et.put(it.next());
				}
				
				etudiants.clear();
				
				try {
					Log.d("LENGTH", "AVANT : " + rootObj.length());
					if(rootObj.length() == 1) {
						Log.d("REMOVE", "obj etudiant removed");
						rootObj.remove("etudiants");
					}
					
					rootObj.put("etudiants", et);
					textView01.setText(rootObj.toString());
					Log.d("LENGTH", "APRES : " + rootObj.length());
					
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		        
			}
		});
	    

	    
        mTextView = (TextView) findViewById(R.id.textView_explanation);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        
        if (mNfcAdapter == null) {
        	//Arrêter! il nous faux du NFC
        	Toast.makeText(this, "Ce péripherique ne supporte pas NFC", Toast.LENGTH_LONG).show();
        	finish();
        	return;
        }
        if (!mNfcAdapter.isEnabled()){
        	mTextView.setText("NFC est désactivé");
        }
        else {
        	mTextView.setText(R.string.explanation);
        }
        try {
			handleIntent(getIntent());
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
    }

    @Override
    protected void onResume(){
    	super.onResume();
    	
    	setupForegroundDispatch(this, mNfcAdapter);
    }
    
    @Override
    protected void onPause(){
    	stopForegroundDispatch(this, mNfcAdapter);
    	super.onPause();
    }
    
    @Override
    protected void onNewIntent(Intent intent){
    	try {
			handleIntent(intent);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    
    private void handleIntent(Intent intent) throws UnsupportedEncodingException{
    	String action  = intent.getAction();
    	if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)){
    		Log.i("Discovery", "Tag Discovered !");
    		String type = intent.getType();
    		if (MIME_TEXT_PLAIN.equals(type)){
    			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
    	    	String nFCID = tag.getId().toString();
    	    	
    	    	new NdefReaderTask().execute(tag);
            } else {
                Log.d(TAG, "Wrong mime type: " + type);
            }
        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
             
            // In case we would still use the Tech Discovered Intent
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String[] techList = tag.getTechList();
            String searchedTech = Ndef.class.getName();
             
            for (String tech : techList) {
                if (searchedTech.equals(tech)) {
                   	Toast.makeText(getApplicationContext(), "NFC TECH ", Toast.LENGTH_SHORT).show(); 
                    new NdefReaderTask().execute(tag);
                    break;
                }
            }
        } else if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
        	String type = intent.getType();
    		if (MIME_TEXT_PLAIN.equals(type)){
    			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
    	    	
    	    	Toast.makeText(getApplicationContext(), "NFC TAG ", Toast.LENGTH_SHORT).show(); 
    			new NdefReaderTask().execute(tag);
    		 } else {
                 Log.d(TAG, "Wrong mime type: " + type);
             }
        } 
    }
    
    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter){
    	final Intent intent  = new Intent(activity.getApplicationContext(),activity.getClass());
    	intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    	
    	final PendingIntent pendingIntent  = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);
    	
    	IntentFilter[] filters = new IntentFilter[1];
    	String[][] techList = new String[][]{};
    	
    	filters[0] = new IntentFilter();
    	filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
    	filters[0].addCategory(Intent.CATEGORY_DEFAULT);
    	try {
    		filters[0].addDataType(MIME_TEXT_PLAIN);
    	} catch (MalformedMimeTypeException e) {
    		throw new RuntimeException("Check your mime type.");
    	}
    	
    	adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }
    
    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter){
    	adapter.disableForegroundDispatch(activity);
    }
    private class NdefReaderTask extends AsyncTask<Tag, Void, String> {
    	@Override
    	protected String doInBackground(Tag... params){
    		Tag tag = params[0];
    		
    		Ndef ndef = Ndef.get(tag);
    		if (ndef == null){
    			return null; // Ndef ne supporte pas ce tag
    		}
    		NdefMessage ndefMessage = ndef.getCachedNdefMessage();
    		 
            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                    try {
                        return readText(ndefRecord);
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "Encodage insupporter", e);
                    }
                }
            }
     
            return null;
        }
         
        private String readText(NdefRecord record) throws UnsupportedEncodingException {
     
            byte[] payload = record.getPayload();
     
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
            int languageCodeLength = payload[0] & 0063;
            return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        }
         
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                mTextView.setText("Read content: " + result);
       	    	Date dt = new Date();
    	    	int hours = dt.getHours();
    	    	int minutes = dt.getMinutes();
    	    	int seconds = dt.getSeconds();

    	    	String cTime = hours+":"+minutes+":"+seconds+"";

    	    	//JSON    	    	
    	    	JSONObject entree = new JSONObject();
    	    	try {
					entree.put("data", result);
					entree.put("time", dt.toString());
					etudiants.add(entree);

				} catch (JSONException e) {
					// TODO Auto-generated catch block
					Log.d("JSON : ","Erreur JSON");
				}
    			//FIN JSON
    	    	
    	    	Toast.makeText(getApplicationContext(), "Tag Contains " + result +"\n NFC NDEF "+"\n At : "+cTime, Toast.LENGTH_SHORT).show(); 
            }
            else {
            	mTextView.setText("VIDE");
            }
    	}
    }

}
