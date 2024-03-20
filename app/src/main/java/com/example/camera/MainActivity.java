package com.example.camera;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 0;
    private static final int YOUR_GALLERY_REQUEST_CODE = 1;
    static File currentPhotoFile;
    static Uri currentPhotoUri;
    static String currentPhotoPath;
    static String currentPhotoFileName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 카메라 버튼에 대한 클릭 리스너 설정
        Button cameraButton = findViewById(R.id.btnCamera);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                capturePhoto();
            }
        });

        // 갤러리 버튼에 대한 클릭 리스너 설정
        Button galleryButton = findViewById(R.id.btnGallery);
        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 갤러리를 열도록 하는 코드를 여기에 추가
                openGallery();
            }

            private void openGallery() {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, YOUR_GALLERY_REQUEST_CODE); // 갤러리에서 이미지를 선택하기 위해 startActivityForResult 사용
            }

        });

    }



    @SuppressLint("QueryPermissionsNeeded")
    public void capturePhoto() {
        try {
            // 카메라 버튼을 찾습니다.
            Button cameraButton = findViewById(R.id.btnCamera);
            if (cameraButton != null) { // 버튼이 null이 아닌 경우에만 계속 진행합니다.
                // 이미 파일을 생성한 후 currentPhotoFile과 currentPhotoUri를 설정합니다.
                currentPhotoFile = createImageFile();
                currentPhotoUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        currentPhotoFile);
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            } else {
                Log.e("capturePhoto", "Camera button is null.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ImageView imageView = findViewById(R.id.imageView);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE && currentPhotoUri != null) {
                // 카메라로부터 촬영한 이미지를 처리하는 코드
                Bitmap antiRotationBitmap = createAntiRotationSampledBitmap(currentPhotoUri, imageView.getWidth(), imageView.getHeight());
                if (antiRotationBitmap != null) {
                    imageView.setImageBitmap(antiRotationBitmap);
                    // 이미지뷰에 이미지가 설정된 후에 레이아웃 파라미터를 조정하여 중앙에 정렬
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) imageView.getLayoutParams();
                    params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                    imageView.setLayoutParams(params);
                    // 갤러리에 이미지 추가
                    galleryAddpic(currentPhotoUri, currentPhotoFileName);
                } else {
                    Log.e("onActivityResult", "Failed to create anti-rotated bitmap.");
                }
            } else if (requestCode == YOUR_GALLERY_REQUEST_CODE && data != null) {
                // 갤러리에서 선택한 이미지를 처리하는 코드
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    // 선택한 이미지를 이미지뷰에 표시
                    imageView.setImageURI(selectedImageUri);
                    // 이미지뷰에 이미지가 설정된 후에 레이아웃 파라미터를 조정하여 중앙에 정렬
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) imageView.getLayoutParams();
                    params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                    imageView.setLayoutParams(params);
                }
            }
        }
    }




    private void galleryAddpic(Uri currentPhotoUri, String currentPhotoFileName) {
    }

    private Uri galleryAddPic(Uri srcImageFileUri ,String srcImageFileName) {
        ContentValues contentValues = new ContentValues();
        ContentResolver contentResolver = getApplicationContext().getContentResolver();
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, srcImageFileName);
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/MyImages"); // 두개의 경로[DCIM/ , Pictures/]만 가능함 , 생략시 Pictures/ 에 생성됨
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 1); //다른앱이 파일에 접근하지 못하도록 함(Android 10 이상)
        Uri newImageFileUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues);

        try {
            AssetFileDescriptor afdInput = contentResolver.openAssetFileDescriptor(srcImageFileUri, "r");
            assert newImageFileUri != null;
            AssetFileDescriptor afdOutput = contentResolver.openAssetFileDescriptor(newImageFileUri, "w");
            assert afdInput != null;
            FileInputStream fis = afdInput.createInputStream();
            assert afdOutput != null;
            FileOutputStream fos = afdOutput.createOutputStream();

            byte[] readByteBuf = new byte[1024];
            while(true){
                int readLen = fis.read(readByteBuf);
                if (readLen <= 0) {
                    break;
                }
                fos.write(readByteBuf,0,readLen);
            }

            fos.flush();
            fos.close();
            afdOutput.close();

            fis.close();
            afdInput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        contentValues.clear();
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0); //다른앱이 파일에 접근할수 있도록 함
        assert newImageFileUri != null;
        contentResolver.update(newImageFileUri, contentValues, null, null);
        return newImageFileUri;
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        //외부저장소 사용
        File imagePath = getExternalFilesDir("images"); //images 디렉토리가 없을시 생성한다.

        //파일생성
        File newFile = File.createTempFile(imageFileName, ".jpg", imagePath);

        currentPhotoFile = newFile;
        currentPhotoFileName = newFile.getName();
        currentPhotoPath = newFile.getAbsolutePath(); //파일의 절대패스를 저장한다.

        try {
            currentPhotoUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    newFile);
        } catch (Exception ex) {
            Log.d("FileProvider", Objects.requireNonNull(ex.getMessage()));
            ex.printStackTrace();
            throw ex;
        }

        return newFile;

    }
    private Bitmap createAntiRotationSampledBitmap(Uri srcImageFileUri, int dstWidth, int dstHeight) {
        ContentResolver contentResolver = getApplicationContext().getContentResolver();
        int orientation = 0;
        try {
            InputStream inputStream = contentResolver.openInputStream(srcImageFileUri);
            if (inputStream != null) {
                ExifInterface exifInterface = new ExifInterface(inputStream);
                orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                inputStream.close();
            } else {
                Log.e("createAntiRotationSampledBitmap", "Failed to open input stream for ExifInterface.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 이미지 회전 및 변환 작업 추가
        Bitmap sampledBitmap = createSampledBitmap(srcImageFileUri, dstWidth, dstHeight);

        // 이미지를 회전합니다.
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                return sampledBitmap;
        }

        try {
            assert sampledBitmap != null;
            Bitmap rotatedBitmap = Bitmap.createBitmap(sampledBitmap, 0, 0, sampledBitmap.getWidth(), sampledBitmap.getHeight(), matrix, true);
            if (rotatedBitmap != sampledBitmap) {
                sampledBitmap.recycle();
            }
            return rotatedBitmap;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }


    private Bitmap createSampledBitmap(Uri srcImageFileUri, int dstWidth, int dstHeight) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(getContentResolver().openInputStream(srcImageFileUri), null, options);

            int srcWidth = options.outWidth;
            int srcHeight = options.outHeight;

            int inSampleSize = 1;
            if (srcWidth > dstWidth || srcHeight > dstHeight) {
                final int halfWidth = srcWidth / 2;
                final int halfHeight = srcHeight / 2;
                while ((halfWidth / inSampleSize) >= dstWidth && (halfHeight / inSampleSize) >= dstHeight) {
                    inSampleSize *= 2;
                }
            }

            options = new BitmapFactory.Options();
            options.inSampleSize = inSampleSize;
            return BitmapFactory.decodeStream(getContentResolver().openInputStream(srcImageFileUri), null, options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
    
