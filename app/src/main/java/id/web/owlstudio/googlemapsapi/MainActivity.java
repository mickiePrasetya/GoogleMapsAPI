package id.web.owlstudio.googlemapsapi;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.android.PolyUtil;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import id.web.owlstudio.googlemapsapi.network.ApiServices;
import id.web.owlstudio.googlemapsapi.network.Constant;
import id.web.owlstudio.googlemapsapi.network.InitRetrofit;
import id.web.owlstudio.googlemapsapi.network.response.Distance;
import id.web.owlstudio.googlemapsapi.network.response.Duration;
import id.web.owlstudio.googlemapsapi.network.response.LegsItem;
import id.web.owlstudio.googlemapsapi.network.response.OverviewPolyline;
import id.web.owlstudio.googlemapsapi.network.response.ResponseRoute;
import id.web.owlstudio.googlemapsapi.network.response.RoutesItem;
import pub.devrel.easypermissions.EasyPermissions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, EasyPermissions.PermissionCallbacks {

    private static final int RC_CAMERA_AND_LOCATION = 99;
    private static final int REQUEST_CODE_PLACE_AUTOCOMPLETE = 999;
    private static final int REQUEST_FROM = 991;
    private static final int REQUEST_TO = 992;

    private LatLng latLngFrom = null;
    private LatLng latLngTo = null;

    @BindView(R.id.tvPickUpFrom)
    TextView tvPickUpFrom;
    @BindView(R.id.tvDestLocation)
    TextView tvDestLocation;
    @BindView(R.id.tvPrice)
    TextView tvPrice;
    @BindView(R.id.tvDistance)
    TextView tvDistance;
    @BindView(R.id.infoPanel)
    LinearLayout infoPanel;
    private GoogleMap mMap;

    // variable penampung koordinat pengguna saat ini
    private LatLng myLatLngLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        ButterKnife.bind(this);

        // Cek apakah GPS aktif atau tidak
        cekGPSStatus();


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


    }

    private void cekGPSStatus() {
        String locationProviders = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if (locationProviders == null || locationProviders.equals("")) {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            Toast.makeText(this, "GPS tidak aktif. Silahkan diaktifkan terlebih dahulu", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "GPS aktif", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        // GET CURRENT LOCATION
        // FusedLocationProviderClient adalah class bawaan dari google-play-services-location di gradle Module App
        FusedLocationProviderClient mFusedLocation = LocationServices.getFusedLocationProviderClient(this);

        mFusedLocation.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    // Simpan lokasi ke myLatLngLocation
                    myLatLngLocation = new LatLng(location.getLatitude(), location.getLongitude());

                    // LatLng sydney = new LatLng(-34, 151);
                    mMap.addMarker(new MarkerOptions().position(myLatLngLocation).title("My Location"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLngLocation, 15F));
                    mMap.getUiSettings().setZoomControlsEnabled(true);
                    mMap.getUiSettings().setMyLocationButtonEnabled(true);

                    // Do it all with location
                    Log.d("My Current location", "Lat : " + location.getLatitude() + " Long : " + location.getLongitude());
                    // Display in Toast
                    // Toast.makeText(MainActivity.this,
                    //        "Lat : " + location.getLatitude() + " Long : " + location.getLongitude(),
                    //        Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // minta permission
        methodRequiresTwoPermission();
    }

    private void methodRequiresTwoPermission() {
        String[] perms = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
        if (EasyPermissions.hasPermissions(this, perms)) {
            // Already have permission, do the thing
            // ...
            // Execute Method
            getCurrentLocation();
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(MainActivity.this, "Membutuhkan akses lokasi", RC_CAMERA_AND_LOCATION, perms);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Some permissions have been granted
        // ...
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Some permissions have been denied
        // ...
    }

    @OnClick(R.id.tvPickUpFrom)
    public void onTvPickUpFromClicked() {
        // Tampilkam Popup Autocomplete place
        showPlaceAutoComplete(REQUEST_FROM);
    }

    @OnClick(R.id.tvDestLocation)
    public void onTvDestLocationClicked() {
        // Tampilkam Popup Autocomplete place
        showPlaceAutoComplete(REQUEST_TO);
    }

    private void showPlaceAutoComplete(int typePickUp) {
        // Filter hanya tempat yang ada di Indonesia
        AutocompleteFilter typeFilter = new AutocompleteFilter.Builder().setCountry("ID").build();

        // Intent implicit untuk memunculkan popup place autocomplete
        try {
            Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                    .setFilter(typeFilter)
                    .build(MainActivity.this);

            // Jalankan implicit intent
            startActivityForResult(intent, typePickUp); // request code untuk mengetahui jenis pick up yang dipilih
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace(); //mencetak error di log
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace(); // mencetak error di log
            Toast.makeText(this, "Layanan Play Services Tidak Tersedia", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Pastikan data ydang didapatkan valid
        if(resultCode == RESULT_OK) {

            // Tampung data dari Place Autocomplete
            Place dataPlace = PlaceAutocomplete.getPlace(this, data);
            if(dataPlace.isDataValid()){
                //tampilkan di log
                Log.d("Autocomplete Place data",dataPlace.toString());

                // Dapatkan detail data
                String placeAddress = dataPlace.getAddress().toString();
                String placeName = dataPlace.getName().toString();
                LatLng placeLatLng = dataPlace.getLatLng();

                // Clear previous marker
                mMap.clear();

                // Cek Request Code
                if(requestCode == REQUEST_FROM){

                    // Simpan place lat long  ke latLngFrom
                    latLngFrom = placeLatLng;

                    // Set Alamat ke textview
                    tvPickUpFrom.setText(String.format("%s - %s", placeName, placeAddress));
                    // Set marker dan zoom ke koordinat
                    mMap.addMarker(new MarkerOptions().position(placeLatLng).title("Pick Up From"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(placeLatLng, 15F));

                } else if(requestCode == REQUEST_TO) {

                    // Simpan place lat long  ke latLngTo
                    latLngTo = placeLatLng;

                    // Set Alamat ke textview
                    tvDestLocation.setText(String.format("%s - %s", placeName, placeAddress));

                    // Set marker dan zoom ke koordinat
                    mMap.addMarker(new MarkerOptions().position(placeLatLng).title("Pick Up To"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(placeLatLng, 15F));
                }

                // Jika user telah memilih titik awal dan titik tujuan
                if (latLngFrom != null && latLngTo != null){


                    //set marker ke 2 koordinat
                    // Buat marker dan zoom ke koordinat
                    mMap.addMarker( new MarkerOptions().position(latLngFrom).title("Pick Up From"));
                    mMap.addMarker( new MarkerOptions().position(latLngTo).title("Pick Up To"));

                    // Jalankan metho routing
                    actionRoute(latLngFrom, latLngTo);

                    // buat map Zoom diantara 2 titik
                    // Bounds Coordinates
                    LatLngBounds.Builder latLngBounds = new LatLngBounds.Builder();

                    // put koordinat
                    latLngBounds.include(latLngFrom);
                    latLngBounds.include(latLngTo);

                    // Bounds Coordinate
                    LatLngBounds bounds = latLngBounds.build();

                    // Menghitung ukuran layar device
                    int width = getResources().getDisplayMetrics().widthPixels;
                    int height = getResources().getDisplayMetrics().heightPixels;
                    int paddingMap = (int) (height * 0.2); // Jarak dari pinggir layar

                    // Zoom Camera
                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, paddingMap);

                    // Animasikan
                    mMap.animateCamera(cu);
                }

            }
        }
    }

    private void actionRoute(LatLng koordinatAwal, LatLng koordinatAkhir) {

        String latLongAwal = koordinatAwal.latitude + "," + koordinatAwal.longitude;
        String latLongAkhir = koordinatAkhir.latitude + "," + koordinatAkhir.longitude;

        // Instance retrofit
        ApiServices api = InitRetrofit.getInstance();

        // Siapkan request
        Call<ResponseRoute> callRoute = api.request_route(
                latLongAwal,
                latLongAkhir,
                Constant.ROUTE_MODE,
                Constant.ROUTE_AVOID,
                Constant.GOOGLE_API_KEY);

        // kirim request
        callRoute.enqueue(new Callback<ResponseRoute>() {
            @Override
            public void onResponse(Call<ResponseRoute> call, Response<ResponseRoute> response) {
                // ketika request sukses
                if(response.isSuccessful()){
                    Log.d("Data routing", response.body().toString());

                    if (response.body().getStatus().equals("OK")){
                        // tampung data route ke variable
                        List<RoutesItem> dataRoutes = response.body().getRoutes();

                        //dapatkan data Polyline
                        OverviewPolyline dataPolyline = dataRoutes.get(0).getOverviewPolyline();

                        // Point untuk rute
                        String polyLinePoint = dataPolyline.getPoints();

                        // decode pointe ke koordinat
                        List<LatLng> decodePath = PolyUtil.decode(polyLinePoint);

                        // Gambar gatis ke maps
                        mMap.addPolyline(new PolylineOptions().addAll(decodePath)
                                .width(8f).color(Color.argb(255,56,167,252)))
                                .setGeodesic(true);

                        // Tampung data legs
                        // Lihat dari response json, cari data Legs
                        List<LegsItem> dataLegs = dataRoutes.get(0).getLegs();

                        // Dapatkan data jarak dan waktu
                        Distance distance = dataLegs.get(0).getDistance();
                        Duration duration = dataLegs.get(0).getDuration();

                        // Set Nilai ke widet textView
                        tvDistance.setText(String.format("%s (%s)", distance.getText(), duration.getText()));

                        // Hitungan Price
                        int hargaParameter = 7500;

                        // Hitung
                        double hasilHarga = ( distance.getValue()/1000 ) * hargaParameter;

                        // format RP dalam Indonesia
                        Locale localeID = new Locale("in", "ID");
                        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(localeID);

                        //Set hasilHarga ke TextView
                        tvPrice.setText(formatRupiah.format(hasilHarga));

                        // tampilkan toast
                        // Toast.makeText(MainActivity.this, dataPolyline.getPoints().toString(), Toast.LENGTH_SHORT).show();
                    } else {
                        // tampilkan toast
                        Toast.makeText(MainActivity.this,"Maaf rute tidak ditemukan", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseRoute> call, Throwable t) {
                // ketika request gagal
                t.printStackTrace(); // cetak error di Logcat
            }
        });
    }
}
