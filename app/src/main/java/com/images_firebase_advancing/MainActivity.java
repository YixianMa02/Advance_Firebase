package com.images_firebase_advancing;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.LongDef;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.images_firebase_advancing.model.Car;
import com.images_firebase_advancing.model.Person;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ValueEventListener,
        OnSuccessListener, OnFailureListener, OnCompleteListener {

    EditText edId,edPhoto;
    ImageView imPhoto;
    Button btnAdd,btnBrowse,btnFind,btnUpload;

    DatabaseReference personDatabase;

    //To upload the image file into Firebase database
    FirebaseStorage storage;
    StorageReference storageReference,sRef;

    ActivityResultLauncher activityResultLauncher;

    Uri filePath;

    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();
    }

    private void initialize() {

        edId = findViewById(R.id.edId);
        imPhoto = findViewById(R.id.imPhoto);
        btnAdd = findViewById(R.id.btnAdd);
        btnBrowse = findViewById(R.id.btnBrowse);
        btnUpload = findViewById(R.id.btnUpload);
        btnFind = findViewById(R.id.btnFind);

        btnAdd.setOnClickListener(this);
        btnBrowse.setOnClickListener(this);
        btnUpload.setOnClickListener(this);
        btnFind.setOnClickListener(this);

        personDatabase = FirebaseDatabase.getInstance().getReference("person");

        // reference Firebase storage
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        getPhoto(result);

                    }
                }
        );
    }

    private void getPhoto(ActivityResult result) {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            filePath = result.getData().getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media
                        .getBitmap(getContentResolver(), filePath);

                imPhoto.setImageBitmap(bitmap);

            } catch(IOException e) {
                Log.d("ADV_FIREBASE", e.getMessage());
            }
        }
    }

    @Override
    public void onClick(View view) {
        int id= view.getId();
        switch (id) {
            case R.id.btnAdd :
                addPerson(view);
                break;
            case R.id.btnFind :
                findPerson();
                break;
            case R.id.btnBrowse:
                selectPhoto();

                break;
            case R.id.btnUpload:
                uploadPhoto();
                break;
        }
    }

    private void uploadPhoto() {
        if (filePath != null) {
            dialog = new ProgressDialog(this);
            dialog.setTitle("Uploading a photo in progress ...");
            dialog.show();

            sRef = storageReference.child("images/" + UUID.randomUUID());
            sRef.putFile(filePath).addOnSuccessListener(this);
            sRef.putFile(filePath).addOnFailureListener(this);


        }
    }

    private void selectPhoto() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        activityResultLauncher.launch(intent.createChooser(intent, "Select a photo"));

    }

    private void addPerson(View view) {
        ArrayList<String> hobbies = new ArrayList<String>();
        hobbies.add("Soccer");
        hobbies.add("Hockey");
        hobbies.add("HandBall");
        hobbies.add("Music");

        Car car = new Car("M200", "Mazda", "Mazda 6");

        Person person = new Person(300, "Richard", "", car, hobbies);

        personDatabase.child("300").setValue(person);
        Snackbar.make(view, "The person with id: 300 has been added successfully", Snackbar.LENGTH_LONG).show();


    }

    private void findPerson() {
        String id = edId.getText().toString();
        DatabaseReference personChild = personDatabase.child(id);
        personChild.addValueEventListener(this);

    }

    @Override
    public void onDataChange(@NonNull DataSnapshot snapshot) {
        String id = edId.getText().toString();
        if (snapshot.exists()){
            // Find the name
            String name = snapshot.child("name").getValue().toString();
            Log.d("ADV_FIREBASE", name);

            // Any sub-document --> Map (Car)
            Map car = (Map) snapshot.child("car").getValue();
            //Log.d("ADV_FIREBASE", "Car brand: " + car.get("brand"));
            // Display All
            Log.d("ADV_FIREBASE", "Car Info: " + car.toString());

            // Any List --> ArrayList
            ArrayList<String> hobbies = (ArrayList<String>) snapshot.child("hobbies").getValue();
            //Log.d("ADV_FIREBASE", "Element 0 " + hobbies.get(0));
            // Display All
            Log.d("ADV_FIREBASE", "All Hobbies: " + hobbies);

            // Find the Photo url
            String photoUrl = snapshot.child("photo").getValue().toString();
            Log.d("ADV_FIREBASE", photoUrl);

            Picasso.with(this).load(photoUrl).placeholder(R.drawable.temp_image).into(imPhoto);

        }
        else
            Toast.makeText(this, "The person with id: " + id + " does not exist in the database.",
                    Toast.LENGTH_LONG).show();

    }

    @Override
    public void onCancelled(@NonNull DatabaseError error) {

    }

    @Override
    public void onSuccess(Object o) {
        Toast.makeText(this, "The photo has been uploaded successfully.", Toast.LENGTH_LONG).show();
        dialog.dismiss();
        // get the photo URL
        sRef.getDownloadUrl().addOnCompleteListener(this);

    }

    @Override
    public void onFailure(@NonNull Exception e) {
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        dialog.dismiss();

    }

    @Override
    public void onComplete(@NonNull Task task) {
        Log.d("ADV_FIREBASE", "The url of the photo is " + task.getResult().toString());

    }
}