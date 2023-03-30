package com.hyper.place.activities


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.hyper.place.R
import com.hyper.place.database.DatabaseHandler
import com.hyper.place.databinding.ActivityAddHappyPlaceBinding
import com.hyper.place.models.HappyPlaceModel
import com.hyper.place.utils.GetAddressFromLatLng
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID


class AddHappyPlaceActivity : AppCompatActivity(){
    private var binding: ActivityAddHappyPlaceBinding? = null
    private var cal = Calendar.getInstance()
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private var saveImageToInternalStorage : Uri? = null
    private var mLatitude : Double = 0.0
    private var mLongitude : Double = 0.0
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private var mHappyPlaceDetails : HappyPlaceModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddHappyPlaceBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setSupportActionBar(binding?.toolbarAddPlace)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding?.toolbarAddPlace?.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if(!Places.isInitialized()){
            Places.initialize(applicationContext, resources.getString(R.string.google_maps_api_key))
        }
//        val placesClient = Places.createClient(this@AddHappyPlaceActivity)

        if(intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)){
            mHappyPlaceDetails = intent.getParcelableExtra(MainActivity.EXTRA_PLACE_DETAILS) as? HappyPlaceModel
        }

        dateSetListener = DatePickerDialog.OnDateSetListener{
            _, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR,year)
            cal.set(Calendar.MONTH,month)
            cal.set(Calendar.DAY_OF_MONTH,dayOfMonth)
            updateDateInView()
        }
        updateDateInView()

        if(mHappyPlaceDetails != null){
            supportActionBar?.title = "Edit Happy Place"
            binding?.etTitle?.setText(mHappyPlaceDetails!!.title)
            binding?.etDescription?.setText(mHappyPlaceDetails!!.description)
            binding?.etDate?.setText(mHappyPlaceDetails!!.date)
            binding?.etLocation?.setText(mHappyPlaceDetails!!.location)
            mLatitude = mHappyPlaceDetails!!.latitude
            mLongitude = mHappyPlaceDetails!!.longitude

            saveImageToInternalStorage = Uri.parse(mHappyPlaceDetails!!.image)

            binding?.ivPlaceImage?.setImageURI(saveImageToInternalStorage)
            binding?.btnSave?.text = "UPDATE"

        }

        binding?.etDate?.setOnClickListener{
            DatePickerDialog(this@AddHappyPlaceActivity,dateSetListener,cal.get(Calendar. YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding?.tvAddImage?.setOnClickListener{
            val pictureDialog = AlertDialog.Builder(this)
            pictureDialog.setTitle("Select Action")
            val pictureDialogItems = arrayOf("Select photo from Gallery","Capture photo from camera")

            pictureDialog.setItems(pictureDialogItems){
                    _,which ->
                when(which){
                    0-> choosePhotoFromGallery()
                    1-> takePhotoFromCamera()
                }
            }
            pictureDialog.show()
        }
        binding?.btnSave?.setOnClickListener{
            when{
                binding?.etTitle?.text.isNullOrEmpty() -> {
                    Toast.makeText(this, "Please enter title", Toast.LENGTH_SHORT).show()
                }
                binding?.etDescription?.text.isNullOrEmpty() -> {
                    Toast.makeText(this, "Please enter description", Toast.LENGTH_SHORT).show()
                }
                binding?.etLocation?.text.isNullOrEmpty() -> {
                    Toast.makeText(this, "Please select location", Toast.LENGTH_SHORT).show()
                }
                saveImageToInternalStorage == null -> {
                    Toast.makeText(this, "Please add image", Toast.LENGTH_SHORT).show()
                }else ->{
                    val happyPlaceModel = HappyPlaceModel(
                        if(mHappyPlaceDetails == null) 0 else mHappyPlaceDetails!!.id,
                        binding?.etTitle?.text.toString(),
                        saveImageToInternalStorage.toString(),
                        binding?.etDescription?.text.toString(),
                        binding?.etDate?.text.toString(),
                        binding?.etLocation?.text.toString(),
                        mLatitude,
                        mLongitude
                    )
                    val dbHandler = DatabaseHandler(this)
                    if(mHappyPlaceDetails == null){
                        val addHappyPlace = dbHandler.addHappyPlace(happyPlaceModel)
                        if(addHappyPlace>0){
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    }else{
                        val updateHappyPlace = dbHandler.updateHappyPlace(happyPlaceModel)
                        if(updateHappyPlace>0){
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    }

                }
            }

        }
        binding?.etLocation?.setOnClickListener{
            try{
                val fields = listOf(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.ADDRESS,
                    Place.Field.LAT_LNG
                )
                val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(this@AddHappyPlaceActivity)
                openAutocompleteLauncher.launch(intent)
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
        binding?.tvSelectCurrentLocation?.setOnClickListener {
            if(!isLocationEnabled()){
                Toast.makeText(this, "Your location provider is turned off. Please turn it on.", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }else{
                accessLocation()
            }
        }
    }


    private fun choosePhotoFromGallery(){
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE)
        permissionLauncherMultipleForGallery.launch(permissions)
    }

    private fun takePhotoFromCamera(){
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA)
        permissionLauncherMultipleForCamera.launch(permissions)
    }

    private fun accessLocation(){
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)
        permissionLauncherMultipleForLocation.launch(permissions)
    }

    private var permissionLauncherMultipleForGallery = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            result ->
        var areaAllGranted = true
        for(isGranted in result.values){
            areaAllGranted = areaAllGranted && isGranted
        }
        if(areaAllGranted){
            val galleryIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            openGalleryLauncher.launch(galleryIntent)
        }else{
            showRationalDialogForPermissions()
        }
    }
    private var permissionLauncherMultipleForCamera = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            result ->
        var areaAllGranted = true
        for(isGranted in result.values){
            areaAllGranted = areaAllGranted && isGranted
        }
        if(areaAllGranted){
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            openCameraLauncher.launch(cameraIntent)
        }else{
            showRationalDialogForPermissions()
        }
    }
    private var permissionLauncherMultipleForLocation = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            result ->
        var areaAllGranted = true
        for(isGranted in result.values){
            areaAllGranted = areaAllGranted && isGranted
        }
        if(areaAllGranted){
            requestNewLocationData()
        }else{
            showRationalDialogForPermissions()
        }
    }


    private val openGalleryLauncher : ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result->
        if(result.resultCode == RESULT_OK && result.data!=null){

            val selectedPhotoUri = result.data!!.data
            try {
                selectedPhotoUri?.let {
                    if(Build.VERSION.SDK_INT < 28) {
                        val bitmap = MediaStore.Images.Media.getBitmap(
                            this.contentResolver,
                            selectedPhotoUri
                        )
                        saveImageToInternalStorage = saveImageToInternalStorage(bitmap)
                        Log.e("saved image: ","path :: $saveImageToInternalStorage")

                        binding?.ivPlaceImage?.setImageBitmap(bitmap)
                    } else {
                        val source = ImageDecoder.createSource(this.contentResolver, selectedPhotoUri)
                        val bitmap = ImageDecoder.decodeBitmap(source)

                        saveImageToInternalStorage = saveImageToInternalStorage(bitmap)
                        Log.e("saved image: ","path :: $saveImageToInternalStorage")

                        binding?.ivPlaceImage?.setImageBitmap(bitmap)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    private val openCameraLauncher : ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result->
        if(result.resultCode == RESULT_OK && result.data!=null){
            val thumbnail = result.data!!.extras!!.get("data") as Bitmap

            saveImageToInternalStorage = saveImageToInternalStorage(thumbnail)
            Log.e("saved image: ","path :: $saveImageToInternalStorage")

            binding?.ivPlaceImage?.setImageBitmap(thumbnail)
        }
    }
    private val openAutocompleteLauncher : ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result->
        if(result.resultCode == RESULT_OK && result.data!=null){
            val place = Autocomplete.getPlaceFromIntent(result.data!!)
            binding?.etLocation?.setText(place.address)
            mLatitude = place.latLng!!.latitude
            mLongitude = place.latLng!!.longitude
        }
    }



    private fun showRationalDialogForPermissions(){
        AlertDialog.Builder(this).setMessage("It looks like you have turned off permissions required " +
                "for this feature.It can be enabled under the" +
                " Application Settings")
            .setPositiveButton("GO TO SETTING"){
                _,_ ->
                try{
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package",packageName,null)
                    intent.data = uri
                    startActivity(intent)
                }catch (e: ActivityNotFoundException){
                    e.printStackTrace()
                }
            }.setNegativeButton("Cancel"){
                dialog, _ -> dialog.dismiss()
            }.show()
    }

    private fun updateDateInView() {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        binding?.etDate?.setText(sdf.format(cal.time).toString())
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap):Uri{
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY,Context.MODE_PRIVATE)
        file = File(file,"${UUID.randomUUID()}.jpg")

        try {
            val stream : OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream)
            stream.flush()
            stream.close()
        }catch (e : IOException){
            e.printStackTrace()
        }
        return Uri.parse(file.absolutePath)
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData(){
        var mLocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,1000)
            .setMinUpdateIntervalMillis(2000).setMaxUpdateDelayMillis(100).build()

        mFusedLocationClient.requestLocationUpdates(mLocationRequest,mLocationCallback, Looper.myLooper())

    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location? = locationResult!!.lastLocation
            mLatitude = mLastLocation!!.latitude
            Log.e("Current Latitude", "$mLatitude")
            mLongitude = mLastLocation!!.longitude
            Log.e("Current Longitude", "$mLongitude")

            val addressTask = GetAddressFromLatLng(this@AddHappyPlaceActivity,mLatitude,mLongitude)
            addressTask.setAddressListener(object : GetAddressFromLatLng.AddressListener{
                override fun onAddressFound(address: String?) {
                    Log.e("Address ::", "" + address)
                    binding?.etLocation?.setText(address)
                    stopLocationUpdates()
                }

                override fun onError() {
                    Log.e("Get Address ::", "Something is wrong...")
                }
            })
            addressTask.getAddress()
        }

    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }
    private fun stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
    }

    override fun onDestroy(){
        super.onDestroy()
        binding = null
    }
    companion object{
        private const val IMAGE_DIRECTORY = "HappyPlaceImages"
    }
}

