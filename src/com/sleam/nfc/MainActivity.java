package com.sleam.nfc;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
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
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
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
    
    public String bin2hex(String s) throws UnsupportedEncodingException{
		byte[] bytes = s.getBytes("utf-8");
		char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
		char[] chars = new char[bytes.length*2];
		for(int i = 0; i < bytes.length; i++) {
		  chars[2*i] = hexDigits[(bytes[i] >> 4) & 0xF];
		  chars[2*i+1] = hexDigits[bytes[i] & 0xF];
		}
		String hex = new String(chars);
		return hex;
	}
    
    private void handleIntent(Intent intent) throws UnsupportedEncodingException{
    	String action  = intent.getAction();
    	if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)){
    		String type = intent.getType();
    		if (MIME_TEXT_PLAIN.equals(type)){
    			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
    	    	String nFCID = tag.getId().toString();   
    	    	Toast.makeText(getApplicationContext(), "NFC id is: "+ bin2hex(nFCID), Toast.LENGTH_SHORT).show(); 
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
                    new NdefReaderTask().execute(tag);
                    break;
                }
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
    			return null; // Ndef ne supporte ce tag
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
            }
    	}
    }

}
