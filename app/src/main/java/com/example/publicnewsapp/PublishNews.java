package com.example.publicnewsapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.time.LocalDate;
import java.util.HashMap;

public class PublishNews extends AppCompatActivity {

    EditText et_newsTitle, et_newsDesc, et_newsAuthor;
    ImageView iv_newsImage;
    Button btn_publishNews;
    private static final int STORAGE_PERMISSION_CODE = 100;
    StorageReference storageReference;
    public static final int IMAGE_REQUEST = 1;
    private Uri imageUri;
    private String imageString;
    private StorageTask uploadTask;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish_news);

        et_newsAuthor = findViewById(R.id.et_newsAuthorName);
        et_newsTitle = findViewById(R.id.et_newsTitle);
        et_newsDesc = findViewById(R.id.et_newsDesc);
        iv_newsImage = findViewById(R.id.iv_newsImage);
        btn_publishNews = findViewById(R.id.btn_publishNews);

        storageReference = FirebaseStorage.getInstance().getReference().child("Uploads");

        iv_newsImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, STORAGE_PERMISSION_CODE);
            }
        });

        btn_publishNews.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                progressDialog.setMessage("Publishing Your Article");
                progressDialog.setTitle("Adding...");
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();
                String author = et_newsAuthor.getText().toString();
                String title = et_newsTitle.getText().toString();
                String desc = et_newsDesc.getText().toString();
                String image = imageString;


                createFood(author, title, desc, image);
            }
        });

        progressDialog = new ProgressDialog(PublishNews.this);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createFood(String author, String title, String desc, String image) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        String userId = firebaseUser.getUid();

        LocalDate currentDate = LocalDate.now();
        String date = currentDate.toString();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child(Register.ARTICLES);
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("author", author);
        hashMap.put("title", title);
        hashMap.put("description", desc);
        hashMap.put("urlToImage", image);
        hashMap.put("publishedAt", date);
        hashMap.put("id", userId);
        reference.push().setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressDialog.dismiss();
                if (task.isSuccessful()) {
                    Toast.makeText(PublishNews.this, "Article Published Successfully", Toast.LENGTH_SHORT).show();
                    imageString = "";
                } else {
                    Toast.makeText(PublishNews.this, "Article Published Failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void openImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, IMAGE_REQUEST);
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = PublishNews.this.getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void uploadImage() {
        final ProgressDialog pd = new ProgressDialog(PublishNews.this);
        pd.setMessage("Uploading...");
        pd.show();
        if (imageUri != null) {
            final StorageReference fileReference = storageReference.child(System.currentTimeMillis() + "." + getFileExtension(imageUri));
            uploadTask = fileReference.putFile(imageUri);
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return fileReference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        try {
                            Uri downloadingUri = task.getResult();
                            Log.d("TAG", "onComplete: uri completed");
                            String mUri = downloadingUri.toString();
                            imageString = mUri;
                            Glide.with(PublishNews.this).load(imageUri).into(iv_newsImage);
                        } catch (Exception e) {
                            Log.d("TAG1", "error Message: " + e.getMessage());
                            Toast.makeText(PublishNews.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        pd.dismiss();
                    } else {
                        Toast.makeText(PublishNews.this, "Failed here", Toast.LENGTH_SHORT).show();
                        pd.dismiss();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(PublishNews.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                }
            });
        } else {
            Toast.makeText(PublishNews.this, "No image Selected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            if (uploadTask != null && uploadTask.isInProgress()) {
                Toast.makeText(PublishNews.this, "Upload in progress", Toast.LENGTH_SHORT).show();
            } else {
                uploadImage();
            }
        }
    }

    public void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(PublishNews.this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(PublishNews.this, new String[]{permission}, requestCode);
        } else {
            openImage();
            Toast.makeText(PublishNews.this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImage();
                Toast.makeText(PublishNews.this, "Storage Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(PublishNews.this, "Storage Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

}