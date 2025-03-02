package app.familygem;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.canhub.cropper.CropImageView;

import java.io.IOException;
import java.io.OutputStream;

// ImageCropperActivity.java
public class ImageCropperActivity extends AppCompatActivity {

    private CropImageView cropImageView;
    private Uri fileDestination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_cropper);

        cropImageView = findViewById(R.id.cropImageView);

        // Retrieve the image URI from the intent
        Uri imageUri = getIntent().getParcelableExtra("IMAGE_URI");
        fileDestination = getIntent().getParcelableExtra("OUTPUT_URI");

        cropImageView.setImageUriAsync(imageUri);

        // Set desired crop options
        cropImageView.setGuidelines(CropImageView.Guidelines.ON);
        // Additional customization as needed

        // Customizing the crop button text (You'll need a custom button in XML)
        Button cropButton = findViewById(R.id.crop_button);
        cropButton.setText(getString(R.string.done));
        cropButton.setOnClickListener(view -> cropImage());
    }

    // Method to handle the crop action
    private void cropImage() {
        Bitmap croppedBitmap = cropImageView.getCroppedImage();
        if (croppedBitmap != null) {
            // Save the cropped image to the specified output URI
            saveBitmapToFile(croppedBitmap, fileDestination);
            Intent resultIntent = new Intent();
            resultIntent.putExtra("CROPPED_IMAGE_URI", fileDestination);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        }
    }

    private void saveBitmapToFile(Bitmap bitmap, Uri fileUri) {
        try {
            OutputStream outputStream = getContentResolver().openOutputStream(fileUri);
            if (outputStream != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}