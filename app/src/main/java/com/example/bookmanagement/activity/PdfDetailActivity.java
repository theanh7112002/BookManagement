package com.example.bookmanagement.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.bookmanagement.MyApplication;
import com.example.bookmanagement.databinding.ActivityPdfDetailBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PdfDetailActivity extends AppCompatActivity {
    private ActivityPdfDetailBinding binding;
    public static final String TAG_DOWN = "DOWNLOAD_TAG";
    String bookId, bookTitle, bookUrl;
    boolean isInMyFavo = false;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //get data from intent
        Intent intent = getIntent();
        bookId = intent.getStringExtra("bookId");

        binding.downloadBtn.setVisibility(View.GONE);

        firebaseAuth = FirebaseAuth.getInstance();
        if(firebaseAuth.getCurrentUser() == null){
            checkisFavorite();
        }

        loadBookDetails();
        MyApplication.incrementBookViewCount(bookId);

        //handle click back
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        //handle click read book
        binding.readBookBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent1 = new Intent(PdfDetailActivity.this, PdfViewActivity.class);
                intent1.putExtra("bookId", bookId);
                startActivity(intent1);
            }
        });

        //handle click tai ve
        binding.downloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG_DOWN, "onClick: Checking...");
                if(ContextCompat.checkSelfPermission(PdfDetailActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                    Log.d(TAG_DOWN, "onClick: Already, CAN DOWN");
                    MyApplication.downloadBook(PdfDetailActivity.this, ""+bookId, ""+bookTitle, ""+bookUrl);

                }else{
                    Log.d(TAG_DOWN, "onClick: Failed request");
                    resultLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            }
        });
        //handle favorite click
        binding.favoriteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (firebaseAuth.getCurrentUser()==null){
                    Toast.makeText(PdfDetailActivity.this, "Đăng nhập để thực hiện chức năng này", Toast.LENGTH_SHORT).show();

                }else {
                    if (isInMyFavo){
                        MyApplication.deleteFavoriteBook(PdfDetailActivity.this, bookId);
                    }else {
                        MyApplication.addFavoriteBook(PdfDetailActivity.this, bookId);
                    }
                }
            }
        });
    }
    //request storage permission
    private ActivityResultLauncher<String> resultLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted ->{
                if(isGranted){
                    Log.d(TAG_DOWN, "Permission...");
                    MyApplication.downloadBook(this, ""+bookId, ""+bookTitle, ""+bookUrl);
                }
                else{
                    Log.d(TAG_DOWN, "Permisson was denied...");
                    Toast.makeText(this, "that bai", Toast.LENGTH_SHORT).show();

                }
    });
    private void loadBookDetails() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get data
                        bookTitle= ""+snapshot.child("title").getValue();
                        String description = ""+snapshot.child("description").getValue();
                        String categoryId = ""+snapshot.child("categoryId").getValue();
                        String viewsCount = ""+snapshot.child("viewsCount").getValue();
                        String downloadsCount = ""+snapshot.child("downloadsCount").getValue();
                        bookUrl = ""+snapshot.child("url").getValue();
                        String timestamp = ""+snapshot.child("timestamp").getValue();

                        //required data is load
                        binding.downloadBtn.setVisibility(View.VISIBLE);

                        MyApplication.loadCategory(
                                ""+categoryId,
                                binding.categoryTv
                        );
                        MyApplication.loadPdfFromUrlSinglePage(
                                ""+bookUrl,
                                ""+bookTitle,
                                binding.pdfView,
                                binding.progressBar,
                                binding.pageTv
                        );
                        MyApplication.laodPdfsize(
                                ""+bookUrl,
                                ""+bookTitle,
                                binding.sizeTv
                        );

                        //set data
                        binding.titleTv.setText(bookTitle);
                        binding.descriptionTv.setText(description);
                        binding.viewsTv.setText(viewsCount.replace("null", "N/A"));
                        binding.downloadsTv.setText(downloadsCount.replace("null", "N/A"));

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
    private void checkisFavorite(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseAuth.getUid()).child("Favorites").child(bookId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        isInMyFavo = snapshot.exists();
                        if(isInMyFavo){
                            binding.favoriteBtn.setText("Bỏ yêu thích");

                        }else {
                            binding.favoriteBtn.setText("Yêu thích");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }
}