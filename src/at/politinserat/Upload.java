package at.politinserat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Upload extends Activity {
	
	public enum UI_STATE {
		SET_CREDENTIALS,
		CHOOSE_PIC_SOURCE,
		UPLOAD
	}
	
	//settings
	private SharedPreferences settings;
	
	private String selectedImagePath;
	
	//image scaling values
	int width = 2048;
	int height = 1536;
	
	//constants
	private static String TAG = "POLITINSERATE";
	private static String SETTING_USERNAME = "USERNAME";
	private static String SETTING_PASSWORD = "PASSWORD";
	private static String URL_LOGIN = "http://inserate.opentution.org/eingabe/rest";
	private static int SELECT_FROM_GAL = 0;
	private static int SELECT_FROM_CAM = 1;
	
	//UI PARTS
	private LinearLayout llChoosePicSource;
	private LinearLayout llUpload;
	private LinearLayout llSetCredentials;
	private Button btnLogin;
	private Button btnUpload;
	private Button btnPicFromGal;
	private Button btnCancel;
	private Button btnLogout;
	private Button btnPicFromCam;
	private TextView tvUsername;
	private TextView tvPassword;
	private ImageView image;
	
	private ProgressDialog pdlg;
	private DialogInterface.OnClickListener dlgRetry;
	
	private Context ctx;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        ctx = this;
        
        //get UI Controlls
        llChoosePicSource = (LinearLayout) findViewById(R.id.llChoosePicSource);
        llUpload = (LinearLayout) findViewById(R.id.llUpload);
        llSetCredentials = (LinearLayout) findViewById(R.id.llSetCredentials);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnLogout = (Button) findViewById(R.id.btnLogout);
        btnUpload = (Button) findViewById(R.id.btnUpload);
        btnPicFromGal = (Button) findViewById(R.id.btnPicFromGal);
        btnPicFromCam = (Button) findViewById(R.id.btnPicFromCam);
        btnCancel = (Button) findViewById(R.id.btnCancel);
        tvUsername = (EditText) findViewById(R.id.tvUsername);
        tvPassword = (EditText) findViewById(R.id.tvPassword);
        image = (ImageView) findViewById(R.id.image);
        
        //assign Events
        btnLogin.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				settings = getSharedPreferences(TAG, 0);
				SharedPreferences.Editor editor = settings.edit();
				
				editor.putString(SETTING_USERNAME, tvUsername.getText().toString());
				editor.putString(SETTING_PASSWORD, tvPassword.getText().toString());
				editor.commit();
				
				init();
			} 
        });
        
        btnUpload.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				upload();
			} 
        });
        
        btnLogout.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				settings = getSharedPreferences(TAG, 0);
				SharedPreferences.Editor editor = settings.edit();
				
				editor.putString(SETTING_USERNAME, "");
				editor.putString(SETTING_PASSWORD, "");
				editor.commit();
				
				init();
			} 
        });
        
        btnCancel.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				setUiState(UI_STATE.CHOOSE_PIC_SOURCE);
			} 
        });
        
        btnPicFromCam.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
		    	File photo = getTempFile(ctx);
		    	selectedImagePath = photo.getAbsolutePath();
		        intent.putExtra(MediaStore.EXTRA_OUTPUT,Uri.fromFile(photo));
		        startActivityForResult(intent, SELECT_FROM_CAM);
			} 
        });
        
        btnPicFromGal.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,""), SELECT_FROM_GAL);
			} 
        });
        
        setUiState(UI_STATE.SET_CREDENTIALS);
        
        init();
    }
    
    private void init() {
    	CheckCredentials();
    }
    
    private void upload(){
    	settings = getSharedPreferences(TAG, 0);
    	final String username = settings.getString(SETTING_USERNAME, "");
    	final String password = settings.getString(SETTING_PASSWORD, "");
    	
    	pdlg = ProgressDialog.show(this,"", "uploading...", true);
    	pdlg.show();
    	
    	class ObjectsLoader extends AsyncTask<Void, String, Void> {
    	
    		boolean success;
    		String msg;

			@Override
			protected Void doInBackground(Void... arg0) {
				if(username.equals("") || password.equals("")){
		    		success = false;
		    	} else {
		    		try {
						JSONObject result = WebService.doUpload(URL_LOGIN , WebService.buildUploadJson(username, password, selectedImagePath).toString());
						
						if(result.getInt("code") == 0){
							success = true;
						} else {
							msg = result.getString("message");
							success = false;
						}
					} catch (IOException e) {
						Log.e(TAG,e.getMessage());
						msg = e.getMessage();
						success = false;
					}  catch (JSONException e) {
						Log.e(TAG,e.getMessage());
						msg = e.getMessage();
						success = false;
					} catch (Exception e) {
						Log.e(TAG,e.getMessage());
						msg = e.getMessage();
						success = false;
					}
		    	}
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				pdlg.dismiss();
    			
    			if(!success){
    				dlgRetry = new DialogInterface.OnClickListener() {
    	        	    @Override
    	        	    public void onClick(DialogInterface dialog, int which) {
    	        	        switch (which){
    	        	        case DialogInterface.BUTTON_POSITIVE:
    	        	        	upload();
    	        	            break;

    	        	        case DialogInterface.BUTTON_NEGATIVE:
    	        	        	setUiState(UI_STATE.CHOOSE_PIC_SOURCE);
    	        	            break;
    	        	        }
    	        	    }
    	        	};
    	        	
    	        	AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
    				builder.setMessage("Fehler: " + msg + " - Erneut versuchen?").setPositiveButton("Ja", dlgRetry)
    				    .setNegativeButton("Nein", dlgRetry).show();

    				
    			} else {
    				Toast.makeText(ctx, "Bild erfolgreich hochgeladen!" , Toast.LENGTH_LONG).show();
    				try{
	    				File f = new File(selectedImagePath);
	    				f.delete();
	    				selectedImagePath = null;
    				} catch(Exception ex) {}
    				setUiState(UI_STATE.CHOOSE_PIC_SOURCE);
    			}
			}
    	
    	}
    	
    	new ObjectsLoader().execute();
    }
    
    private void CheckCredentials(){
    	settings = getSharedPreferences(TAG, 0);
    	final String username = settings.getString(SETTING_USERNAME, "");
    	final String password = settings.getString(SETTING_PASSWORD, "");
    	
    	pdlg = ProgressDialog.show(this,"", "logging in...", true);
    	pdlg.show();
    	
    	class ObjectsLoader extends AsyncTask<Void, String, Void> {
    	
    		boolean success;
    		String msg;

			@Override
			protected Void doInBackground(Void... arg0) {
				if(username.equals("") || password.equals("")){
		    		success = false;
		    	} else {
		    		try {
						if(!WebService.doLogin(URL_LOGIN , WebService.buildLoginJson(username, password).toString())){
							Log.e(TAG,"Login fehlgeschlagen!");
							msg = "Login fehlgeschlagen!";
							success = false;
						} else {
							success = true;
						}
					} catch (IOException e) {
						Log.e(TAG,e.getMessage());
						msg = e.getMessage();
						success = false;
					}  catch (JSONException e) {
						Log.e(TAG,e.getMessage());
						msg = e.getMessage();
						success = false;
					} catch (Exception e) {
						Log.e(TAG,e.getMessage());
						msg = e.getMessage();
						success = false;
					}
		    	}
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				pdlg.dismiss();
    			
    			if(!success){
    				if(msg != null && !msg.equals("")){
    					Toast.makeText(ctx, msg , Toast.LENGTH_LONG).show();
    				}
    				setUiState(UI_STATE.SET_CREDENTIALS);
    			} else {
    				setUiState(UI_STATE.CHOOSE_PIC_SOURCE);
    			}
			}
    	
    	}
    	
    	new ObjectsLoader().execute();
    }
    
    private void setUiState(UI_STATE uiState){
    	switch (uiState) {
    	case SET_CREDENTIALS:
    		llUpload.setVisibility(LinearLayout.GONE);
    		llChoosePicSource.setVisibility(LinearLayout.GONE);
    		llSetCredentials.setVisibility(LinearLayout.VISIBLE);
    		tvUsername.setText("");
    		tvPassword.setText("");
    		break;
    	case CHOOSE_PIC_SOURCE:
    		llUpload.setVisibility(LinearLayout.GONE);
    		llChoosePicSource.setVisibility(LinearLayout.VISIBLE);
    		llSetCredentials.setVisibility(LinearLayout.GONE);
    		break;
    	case UPLOAD:
    		llUpload.setVisibility(LinearLayout.VISIBLE);
    		llChoosePicSource.setVisibility(LinearLayout.GONE);
    		llSetCredentials.setVisibility(LinearLayout.GONE);
    		break;
    	}
    	
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_FROM_GAL) {
            	try{
	                Uri selectedImageUri = data.getData();
	                selectedImagePath = getPath(selectedImageUri);
	                
	                scaleImage();
	                
	                Drawable d = Drawable.createFromPath(selectedImagePath);
	                image.setImageDrawable(d);
            	} catch(Exception ex){
            		Toast.makeText(ctx, "Fehler: " + ex.getMessage(), Toast.LENGTH_LONG);
            		setUiState(UI_STATE.CHOOSE_PIC_SOURCE);
            		selectedImagePath = null;
            	}
            }
            if (requestCode == SELECT_FROM_CAM) {
                // append to GUI
            	try{
	            	scaleImage();
	            	
	                Drawable d = Drawable.createFromPath(selectedImagePath);
	                image.setImageDrawable(d);
            	} catch(Exception ex){
            		Toast.makeText(ctx, "Fehler: " + ex.getMessage(), Toast.LENGTH_LONG);
            		setUiState(UI_STATE.CHOOSE_PIC_SOURCE);
            		selectedImagePath = null;
            	}
            }
            setUiState(UI_STATE.UPLOAD);
        }
    }

    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
    
    private void scaleImage() throws FileNotFoundException {
    	Bitmap bitm = BitmapFactory.decodeFile(selectedImagePath);
        
        int origWidth = bitm.getWidth();
        int origHeight = bitm.getHeight();
        
        float scaleWidth;
        float scaleHeight;
        
        // calculate scale-values..
        if (origWidth > origHeight) { // wide!
            scaleWidth = ((float) width) / origWidth;
            scaleHeight = scaleWidth;
        } else { // high!
        	scaleHeight = ((float) height) / origHeight;
        	scaleWidth = scaleHeight;
        } 
        
        Matrix matrix = new Matrix();
        
        matrix.postScale(scaleWidth, scaleHeight);
        
        Bitmap resizedBitm = Bitmap.createBitmap(bitm,0,0,origWidth,origHeight,matrix,true);
        
        FileOutputStream out = null;
        
        out = new FileOutputStream(selectedImagePath);
		resizedBitm.compress(Bitmap.CompressFormat.JPEG, 80, out);
    }
    
    private File getTempFile(Context context){
  	  //it will return /sdcard/image.tmp
  	  final File path = new File( Environment.getExternalStorageDirectory(), context.getPackageName() );
  	  if(!path.exists()){
  	    path.mkdir();
  	  }
  	  return new File(path, "politinserate.jpg");
  }
}