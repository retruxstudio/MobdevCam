package com.retrux.mobdevcam;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    public static final int CAMERA_REQUEST_CODE = 102;
    private final int PICK_IMAGE_REQUEST = 22;
    private StorageReference storeRef;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private String nameString;
    private Uri fileURI;
    ImageView imageView;
    TextView toastMsg, uploadMsg;
    Button camBtn, uploadBtn, galleryBtn;
    String fileString;
    Toast toast;
    View layout, lottieLayout;
    LottieAnimationView focusAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LayoutInflater inflater = getLayoutInflater();
        layout = inflater.inflate(R.layout.toast_layout, findViewById(R.id.toastLayout));
        lottieLayout = inflater.inflate(R.layout.lottie_layout, findViewById(R.id.lottieLayout));

        layout.setBackgroundResource(R.drawable.toast_bg);
        lottieLayout.setBackgroundResource(R.drawable.toast_bg);

        toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP,0,350);

        storage= FirebaseStorage.getInstance();
        storageReference=storage.getReference();

        focusAnim = findViewById(R.id.focusAnimation);
        uploadMsg = lottieLayout.findViewById(R.id.uploadMsg);
        toastMsg = layout.findViewById(R.id.toastMsg);
        imageView = findViewById(R.id.imageView);
        camBtn = findViewById(R.id.camBtn);
        uploadBtn = findViewById(R.id.uploadBtn);
        galleryBtn = findViewById(R.id.galleryBtn);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        if (ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE) !=PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA,  Manifest.permission.READ_EXTERNAL_STORAGE}, 10);
        }

        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SelectImage();
            }
        });

        camBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                captureImage();
            }
        });

        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadFile();
            }
        });
    }

    private void SelectImage()
    {

        Intent intent = new Intent();
        String time = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        nameString = "MobdevCam_" + time + ".jpg";
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);

    }

    private File createImageFile() throws IOException {
        String time = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        nameString = "MobdevCam_" + time;
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                nameString,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        fileString = image.getAbsolutePath();
        return image;
    }

    private void captureImage() {

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e("error",  ex.toString());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                fileURI = FileProvider.getUriForFile(this,
                        "com.retrux.mobdevcam.fileprovider",
                        photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileURI);
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
            }
        }

    }

    private void uploadFile() {

        if (fileURI != null) {

            String type = ".jpg";
            if (nameString.contains(".jpg")) {
                storeRef = storageReference.child(nameString);
            } else {
                storeRef = storageReference.child(nameString + ".jpg");
            }

            toast.setView(lottieLayout);
            uploadMsg.setText(R.string.uploaded);

            try {
                storeRef.putFile(fileURI);

                toast.show();

                Drawable id = getResources().getDrawable(R.drawable.ic_emptyimage);
                imageView.setImageDrawable(id);

                fileURI = null;
                focusAnim.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
            }

        } else {
            toast.setView(layout);
            toastMsg.setText(R.string.toastMsg);
            toast.show();

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CAMERA_REQUEST_CODE){
            if(resultCode == Activity.RESULT_OK){
                File f = new File(fileString);
                imageView.setImageURI(Uri.fromFile(f));
                Log.d("tag", "ABsolute Url of Image is " + Uri.fromFile(f));

                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(f);
                mediaScanIntent.setData(contentUri);
                this.sendBroadcast(mediaScanIntent);
                focusAnim.setVisibility(View.GONE);
            }

        }

        if (requestCode == PICK_IMAGE_REQUEST
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {

            // Get the Uri of data
            fileURI = data.getData();
            try {

                imageView.setImageURI(fileURI);
                focusAnim.setVisibility(View.GONE);

            } catch (Exception e) {
                Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
            }
        }

    }
}