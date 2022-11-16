package com.example.marksapp;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApiNotAvailableException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.model.Unit;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {


    public static boolean isGranted = false;

    private static double destLat = 0, destLng = 0;

    CardView MapCard, HomeCard, WorkCard;
    SwitchCompat MeasurementSwitch;

    FirebaseAuth mAuth;
    DatabaseReference dbRef;
    private Users curU;
    private List<LandmarksModel> favs = new ArrayList<>();


    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ProgressBar pb = (ProgressBar) findViewById(R.id.levelProgressPB);
        TextView email = (TextView) findViewById(R.id.mainEmailtxt);
        TextView level = (TextView) findViewById(R.id.mainLeveltxt);
        TextView progress = (TextView) findViewById(R.id.mainProgresstxt);

        mAuth  = FirebaseAuth.getInstance(); //need firebase authentication instance
        dbRef = FirebaseDatabase.getInstance().getReference();

        dbRef.child("Users").child(mAuth.getUid()).addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                curU = snapshot.getValue(Users.class);

               // List<LandmarksModel> favs = new ArrayList<>();
                for (DataSnapshot ds: snapshot.child("savedLandmarks").getChildren()){
                    favs.add(ds.getValue(LandmarksModel.class));
                    Log.d("123456", "onDataChange: " + ds.getValue(LandmarksModel.class).getLmName() );
                }
                //favs = curU.getSavedLandmarks();
                FirebaseUser fu = mAuth.getCurrentUser();

                email.setText(fu.getEmail());
                level.setText("Level: " +curU.getLevel());
                Log.d("123456", curU.getTotalTravelDistance() + " / " + curU.getLevelGoal());
                long perc =  curU.getTotalTravelDistance() * 100 / curU.getLevelGoal();
                progress.setText(perc + "%");

                pb.setMax((int) curU.getLevelGoal());
                pb.setProgress((int) curU.getTotalTravelDistance());

            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        CheckPermissions();
        InitUI();

    }


    public void InitUI(){
        MapCard = (CardView) findViewById(R.id.Mapcard_id);
        HomeCard = (CardView) findViewById(R.id.Homecard_id);
        WorkCard = (CardView) findViewById(R.id.Workcard_id);
        MeasurementSwitch = (SwitchCompat) findViewById(R.id.SwitchID);

        MapCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent GoToMap = new Intent(getApplicationContext(), MapsActivity.class);

                startActivity(GoToMap);
            }
        });
        HomeCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GetDestLatLng(1);
                Intent GoToMapHome = new Intent(getApplicationContext(), MapsActivity.class);
                GoToMapHome.putExtra("DestLat", destLat);
                GoToMapHome.putExtra("DestLng", destLng);
                GoToMapHome.putExtra("Mode", 1);
                if (curU.getMeasurementPref() == Unit.METRIC)
                    GoToMapHome.putExtra("MUnit", 0);
                else GoToMapHome.putExtra("MUnit", 1);
                startActivity(GoToMapHome);
            }
        });
        WorkCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GetDestLatLng(2);
                Intent GoToMapWork = new Intent(getApplicationContext(), MapsActivity.class);
                GoToMapWork.putExtra("DestLat", destLat);
                GoToMapWork.putExtra("DestLng", destLng);
                GoToMapWork.putExtra("Mode", 2);
                if (curU.getMeasurementPref() == Unit.METRIC)
                    GoToMapWork.putExtra("MUnit", 0);
                else GoToMapWork.putExtra("MUnit", 1);
                startActivity(GoToMapWork);
            }
        });

        MeasurementSwitch.setText(MeasurementSwitch.getTextOff());
        MeasurementSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
                if (MeasurementSwitch.isChecked()){
                    MeasurementSwitch.setText(MeasurementSwitch.getTextOn());
                    Unit pref = Unit.IMPERIAL;
                    reference.child(firebaseUser.getUid()).child("measurementPref").setValue(Unit.IMPERIAL).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                Toast.makeText(getApplicationContext(), "Measurement preference is now " + MeasurementSwitch.getTextOn(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    MeasurementSwitch.setText(MeasurementSwitch.getTextOff());
                    Unit pref = Unit.METRIC;
                    reference.child(firebaseUser.getUid()).child("measurementPref").setValue(Unit.METRIC).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                Toast.makeText(getApplicationContext(), "Measurement preference is now " + MeasurementSwitch.getTextOff(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });


    }

    private void GetDestLatLng(int code) {
        if (!favs.isEmpty()){
            loop: for (LandmarksModel lm : favs){
                switch (code) {
                    case 1:
                        if (lm.isHome()){
                            destLat = lm.getLmLat();
                            destLng = lm.getLmLng();
                            break loop;
                        }
                        break;
                    case 2:
                        if (lm.isWork()){
                            destLat = lm.getLmLat();
                            destLng = lm.getLmLng();
                            break loop;
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void CheckPermissions() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)){
            isGranted = true;
        } else {
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 0: if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            }
            break;
            default:
                Toast.makeText(this, "Permissions Not Granted", Toast.LENGTH_SHORT).show();
        }
    }
}