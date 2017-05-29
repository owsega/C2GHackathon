package com.owsega.c2ghackathon;

import android.Manifest.permission;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.IdpResponse;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.flipboard.bottomsheet.commons.ImagePickerSheetView;
import com.flipboard.bottomsheet.commons.ImagePickerSheetView.Builder;
import com.flipboard.bottomsheet.commons.ImagePickerSheetView.ImagePickerTile;
import com.flipboard.bottomsheet.commons.ImagePickerSheetView.ImageProvider;
import com.flipboard.bottomsheet.commons.ImagePickerSheetView.OnTileSelectedListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask.TaskSnapshot;
import com.owsega.c2ghackathon.User.Status;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.owsega.c2ghackathon.Utils.setError;
import static com.owsega.c2ghackathon.Utils.snack;

/**
 * For getting additional info from the user, after his/her email has
 * been ascertained and Firebase is logged in.
 */
public class UserInfoActivity extends AppCompatActivity {

    private static final int STORAGE_RC = 12;
    private static final int LOAD_IMAGE_RC = 13;
    private static final int IMAGE_CAPTURE_RC = 14;
    private static final String TAG = "UserInfoActivity";
    private static final String USERS = "users";

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.profile_pic)
    ImageView profilePic;
    @BindView(R.id.first_name)
    TextInputEditText firstName;
    @BindView(R.id.last_name)
    TextInputEditText lastName;
    @BindView(R.id.bottomsheet)
    BottomSheetLayout bottomsheet;
    FirebaseUser user;
    StorageReference storageReference;
    DatabaseReference dbReference;
    @BindView(R.id.dob)
    TextView dob;
    @BindView(R.id.address)
    TextInputEditText address;
    @BindView(R.id.occupation)
    TextInputEditText occupation;
    @BindView(R.id.save)
    Button save;
    private Uri cameraImageUri;
    private Uri userImageUri;

    public static Intent createIntent(Context context, IdpResponse response) {
        return new Intent(context, UserInfoActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signed_in);
        ButterKnife.bind(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bottomsheet.setPeekOnDismiss(true);

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            storageReference = FirebaseStorage.getInstance().getReference(user.getUid());
            dbReference = FirebaseDatabase.getInstance().getReference().child(USERS);
        } else {
            // todo return user to Registrant Activity
            finish();
        }
    }

    /**
     * open gallery for selecting user image.
     */
    @OnClick(R.id.profile_pic)
    public void profilePicClicked() {
        boolean needsPermissions = ActivityCompat.checkSelfPermission(this,
                permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
        if (needsPermissions) {
            requestStoragePermission();
        } else {
            showSheetView();
        }
    }

    @TargetApi(VERSION_CODES.JELLY_BEAN)
    private void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, new String[]{permission.WRITE_EXTERNAL_STORAGE}, STORAGE_RC);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission.WRITE_EXTERNAL_STORAGE}, STORAGE_RC);
        }
    }

    /**
     * Show an {@link ImagePickerSheetView}
     */
    private void showSheetView() {
        ImagePickerSheetView sheetView = new Builder(this)
                .setMaxItems(30)
                .setShowCameraOption(createCameraIntent() != null)
                .setShowPickerOption(createPickIntent() != null)
                .setImageProvider(new ImageProvider() {
                    @Override
                    public void onProvideImage(ImageView imageView, Uri imageUri, int size) {
                        Glide.with(UserInfoActivity.this)
                                .load(imageUri)
//                                .centerCrop()
                                .crossFade()
                                .into(imageView);
                    }
                })
                .setOnTileSelectedListener(new OnTileSelectedListener() {
                    @Override
                    public void onTileSelected(ImagePickerTile selectedTile) {
                        bottomsheet.dismissSheet();
                        if (selectedTile.isCameraTile()) {
                            dispatchTakePictureIntent();
                        } else if (selectedTile.isPickerTile()) {
                            startActivityForResult(createPickIntent(), LOAD_IMAGE_RC);
                        } else if (selectedTile.isImageTile()) {
                            uploadSelectedImage(selectedTile.getImageUri());
                        } else {
                            snack(bottomsheet, R.string.general_error);
                        }
                    }
                })
                .setTitle(R.string.select_image)
                .create();

        bottomsheet.showWithSheetView(sheetView);
    }

    /**
     * This checks to see if there is a suitable activity to handle the {@link MediaStore#ACTION_IMAGE_CAPTURE}
     * intent and returns it if found. {@link MediaStore#ACTION_IMAGE_CAPTURE} is for letting another app take
     * a picture from the camera and store it in a file that we specify.
     *
     * @return A prepared intent if found.
     */
    @Nullable
    private Intent createCameraIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            return takePictureIntent;
        } else {
            return null;
        }
    }

    /**
     * This checks to see if there is a suitable activity to handle the `ACTION_PICK` intent
     * and returns it if found. {@link Intent#ACTION_PICK} is for picking an image from an external app.
     *
     * @return A prepared intent if found.
     */
    @Nullable
    private Intent createPickIntent() {
        Intent picImageIntent = new Intent(Intent.ACTION_PICK, Media.EXTERNAL_CONTENT_URI);
        if (picImageIntent.resolveActivity(getPackageManager()) != null) {
            return picImageIntent;
        } else {
            return null;
        }
    }

    /**
     * This utility function combines the camera intent creation and image file creation, and
     * ultimately fires the intent.
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = createCameraIntent();
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent != null) {
            // Create the File where the photo should go
            try {
                File imageFile = createImageFile();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
                startActivityForResult(takePictureIntent, IMAGE_CAPTURE_RC);
            } catch (IOException e) {
                snack(bottomsheet, R.string.image_creation_failed);
            }
        }
    }

    private void uploadSelectedImage(Uri uri) {
        profilePic.setImageURI(uri);
        // Upload to Cloud Storage
        storageReference.putFile(uri)
                .addOnSuccessListener(this, new OnSuccessListener<TaskSnapshot>() {
                    @Override
                    public void onSuccess(TaskSnapshot taskSnapshot) {
                        Toast.makeText(UserInfoActivity.this, "Image uploaded",
                                Toast.LENGTH_SHORT).show();
                        Uri path = taskSnapshot.getMetadata().getDownloadUrl();
                        Log.d(TAG, "uploadPhoto:onSuccess:" + path);
                        showSelectedImage(path);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "uploadPhoto:onError", e);
                        Toast.makeText(UserInfoActivity.this, "Upload failed",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * For images captured from the camera, we need to create a File first to tell the camera
     * where to store the image.
     *
     * @return the File created for the image to be store under.
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        cameraImageUri = Uri.fromFile(imageFile);
        return imageFile;
    }

    void showSelectedImage(Uri url) {
        Glide.with(this)
                .load(userImageUri = url)
                .centerCrop()
                .crossFade()
                .into(profilePic);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            Uri selectedImage = null;
            if (requestCode == LOAD_IMAGE_RC && data != null) {
                selectedImage = data.getData();
                if (selectedImage == null) {
                    snack(bottomsheet, R.string.general_error);
                }
            } else if (requestCode == IMAGE_CAPTURE_RC) {
                selectedImage = cameraImageUri;
            }

            uploadSelectedImage(selectedImage);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == STORAGE_RC) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showSheetView();
            } else {
                snack(bottomsheet, R.string.grant_pictures_permissn_in_settings);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @OnClick(R.id.save)
    public void saveProfile() {
        String fname = firstName.getText().toString().trim();
        if (TextUtils.isEmpty(fname)) {
            setError(firstName, "Please enter a name");
            return;
        }

        String lname = lastName.getText().toString().trim();
        if (TextUtils.isEmpty(lname)) {
            setError(lastName, "Please enter a name");
            return;
        }

        if (userImageUri == null) snack(bottomsheet, R.string.choose_img_eror);
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(fname + " " + lname)
                .setPhotoUri(userImageUri)
                .build();
        user.updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User profile updated.");
                        }
                    }
                });

        User thisUser = new User(user.getUid())
                .setFirstName(fname)
                .setLastName(lname)
                .setProfilePicUrl(userImageUri.toString())
                .setAddress(address.getText().toString())
                .setDateOfBirth(dob.getText().toString())
                .setEmail(user.getEmail())
                .setOccupation(occupation.getText().toString())
                .setStatus(Status.REGISTRANT);
        dbReference.child(user.getUid()).setValue(thisUser);
    }

    @OnClick(R.id.dob)
    public void showDatePicker() {
        new DatePickerHelper().showDatePicker(dob, null, null, System.currentTimeMillis());
    }
}
