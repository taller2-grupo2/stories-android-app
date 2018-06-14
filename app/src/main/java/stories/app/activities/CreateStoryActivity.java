package stories.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;

import java.io.File;
import java.util.Calendar;

import stories.app.R;
import stories.app.activities.images.ImageFiltersActivity;
import stories.app.models.Story;
import stories.app.services.FileService;
import stories.app.services.LocationService;
import stories.app.services.StoryService;
import stories.app.utils.FileUtils;
import stories.app.utils.LocalStorage;

public class CreateStoryActivity extends AppCompatActivity {

    private static final int START_FILE_UPLOAD = 5;
    private static final int FINISH_IMAGE_FILTER = 6;
    private enum SUPPORTED_IMAGE_FORMATS { jpeg, jpg, png }
    private enum SUPPORTED_VIDEO_FORMATS { mov, mp4 }
    private static final int MAX_FILE_SIZE_MB = 15;
    private LocationService locationService = new LocationService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_story);

        Button createStoryButton = this.findViewById(R.id.createStoryButton);
        createStoryButton.setOnClickListener(new CreateStoryHandler());

        this.locationService.startLocationUpdates(this);
    }

    private boolean isSupportedImage(String fileExtension) {
        for (SUPPORTED_IMAGE_FORMATS imageFormat : SUPPORTED_IMAGE_FORMATS.values()) {
            if (imageFormat.name().equals(fileExtension)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSupportedVideo(String fileExtension) {
        for (SUPPORTED_VIDEO_FORMATS videoFormat : SUPPORTED_VIDEO_FORMATS.values()) {
            if (videoFormat.name().equals(fileExtension)) {
                return true;
            }
        }
        return false;
    }

    private boolean validatesFileSizeRestriction(String filePath){
        File file = new File(filePath);
        double fileBytes = file.length();
        double fileMegabytes = (fileBytes / (1024 * 1024));
        return fileMegabytes <= MAX_FILE_SIZE_MB;
    }

    private boolean isSupportedFormat(String fileExtension){
        return isSupportedImage(fileExtension) || isSupportedVideo(fileExtension);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == START_FILE_UPLOAD) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                Uri selectedImage = data.getData();
                String filePath = FileUtils.getFilePathFromUri(this, selectedImage);
                String fileExtension = filePath.substring(filePath.lastIndexOf(".") + 1);

                if (isSupportedFormat(fileExtension)){
                    if (validatesFileSizeRestriction(filePath)) {
                        if (isSupportedImage(fileExtension)) {
                            Intent navigationIntent = new Intent(CreateStoryActivity.this, ImageFiltersActivity.class);
                            navigationIntent.putExtra("imageUri", filePath);
                            startActivityForResult(navigationIntent, FINISH_IMAGE_FILTER);
                        } else {
                            uploadStory(filePath);
                        }
                    }
                    else{
                        startFileUpload("File selected is bigger than " + MAX_FILE_SIZE_MB + " MB");
                    }
                }
                else{
                    startFileUpload("File format not supported.");
                }
            }
        }
        else if (requestCode == FINISH_IMAGE_FILTER){
            if (resultCode == AppCompatActivity.RESULT_OK) {
                String filePath = FileUtils.getFilePathFromUri(this, Uri.parse(data.getDataString()));
                uploadStory(filePath);
            }
        }
    }

    private void uploadStory(String filePath) {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.createStoryLayout), "Uploading story...", Snackbar.LENGTH_INDEFINITE);
        ViewGroup contentLay = (ViewGroup) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text).getParent();
        ProgressBar item = new ProgressBar(this);
        item.setIndeterminate(true);
        contentLay.addView(item,0);
        snackbar.show();

        EditText title = findViewById(R.id.createStoryTitle);
        EditText description = findViewById(R.id.createStoryDescription);
        CheckBox isQuickStory = findViewById(R.id.createStoryIsQuickStory);
        RadioGroup visibility = findViewById(R.id.createStoryVisibility);

        int selectedVisibilityId = visibility.getCheckedRadioButtonId();
        String visibilityType = selectedVisibilityId == R.id.createStoryVisibilityIsPublic ? "public" : "private";

        Story story = new Story();
        story.userId = LocalStorage.getUser().id;
        story.title = title.getText().toString();
        story.description = description.getText().toString();
        story.isQuickStory = isQuickStory.isSelected();
        story.visibility = visibilityType;
        story.timestamp = Calendar.getInstance().getTime().toString();
        story.location = this.locationService.getLocation();
        try {
            Story newStory = new CreateStoryTask().execute(story).get();
            new UploadFileTask(filePath).execute(newStory.id);
        }
        catch(Exception e){
            startFileUpload("There was an error uploading the file.");
        }
    }

    protected class CreateStoryHandler implements View.OnClickListener {

        public void onClick(View v){
            startFileUpload();
        }
    }

    private void startFileUpload(String messageToDisplay){
        Snackbar snackbar = Snackbar
                .make(findViewById(R.id.createStoryLayout), messageToDisplay, Snackbar.LENGTH_LONG);
        snackbar.show();
        startFileUpload();
    }

    private void startFileUpload() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*, video/*");
        //photoPickerIntent.setTypeAndNormalize("image/jpeg, image/jpg, image/png, video/mp4");
        startActivityForResult(photoPickerIntent, START_FILE_UPLOAD);
    }

    protected class CreateStoryTask extends AsyncTask<Story, Void, Story> {
        private StoryService storyService = new StoryService();

        public CreateStoryTask() {}

        protected void onPreExecute() {
            Button createStoryButton = findViewById(R.id.createStoryButton);
            createStoryButton.setEnabled(false);
        }

        protected Story doInBackground(Story... params) {
            Story storyToUpload = params[0];
            return storyService.createStory(storyToUpload);
        }
    }

    protected class UploadFileTask extends AsyncTask<String, Void, Story> {
        private File file;
        private FileService fileService = new FileService();

        public UploadFileTask(String filePath) {
            this.file = new File(filePath);
        }

        protected Story doInBackground(String... storyId) {
            return fileService.uploadFileToStory(storyId[0], this.file);
        }

        protected void onPostExecute(Story result) {
            Button createStoryButton = findViewById(R.id.createStoryButton);
            createStoryButton.setEnabled(true);

            if (result != null) {
                // Navigate to Home page
                Intent navigationIntent = new Intent(CreateStoryActivity.this, HomeActivity.class);
                startActivity(navigationIntent);
            }
        }
    }
}
