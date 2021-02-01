package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Location
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.*

/**
 * Adapts code from https://github.com/googlemaps/android-samples/tree/master/tutorials/kotlin/CurrentPlaceDetailsOnMap
 */
class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()

    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var googleMap: GoogleMap

    private var locationPermissionDenied = false

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var lastKnownLocation: Location? = null

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private val defaultLocation = LatLng(-33.8523341, 151.2106085)

    private var selectedPoi: PointOfInterest? = null
    private var selectedMarker: Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        binding.selectLocationBtn.setOnClickListener {
            onLocationSelected()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.setContentDescription(getString(R.string.google_maps_poi_selection))

        enableMyLocation()
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (isFineAccessLocationGranted()) {
            googleMap.isMyLocationEnabled = true
            googleMap.uiSettings.isMyLocationButtonEnabled = true

            getDeviceLocation(googleMap)

            setPoiClickListener(googleMap)

            setLongClickListener(googleMap)

        } else {
            @Suppress("DEPRECATION")
            requestPermissions(
                arrayOf(ACCESS_FINE_LOCATION),
                ACCESS_FINE_LOCATION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == ACCESS_FINE_LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                enableMyLocation()
            } else {
                locationPermissionDenied = true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (locationPermissionDenied) {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.location_access_denied_title))
                .setMessage(getString(R.string.location_access_denied_title))
                .setPositiveButton(R.string.ok) { dialog, _ -> dialog.cancel() }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun onLocationSelected() {
        selectedPoi?.apply {
            Timber.d("Selected POI for reminder: $name: Latitude=${latLng.latitude}; Longitude=${latLng.longitude}")
            _viewModel.setSelectedPoi(this)
            navigateToSaveReminderFragment()
        }
    }

    private fun navigateToSaveReminderFragment() {
        _viewModel.navigationCommand.postValue(
            NavigationCommand.To(
                SelectLocationFragmentDirections.actionSelectLocationFragmentToSaveReminderFragment()
            )
        )
    }

    private fun isFineAccessLocationGranted() =
        ContextCompat.checkSelfPermission(
            requireContext(),
            ACCESS_FINE_LOCATION
        ) == PERMISSION_GRANTED

    private fun setPoiClickListener(map: GoogleMap) {
        map.setOnPoiClickListener {
            selectedPoi = it
            selectedMarker?.remove()
            selectedMarker = map.addMarker(
                MarkerOptions()
                    .position(it.latLng)
                    .title(it.name)
            )
        }
    }

    private fun setLongClickListener(map: GoogleMap) {
        map.setOnMapLongClickListener {
            selectedPoi = PointOfInterest(
                it,
                UUID.randomUUID().toString(),
                "Lat: ${it.latitude}, Long: ${it.longitude}"
            )

            selectedMarker?.remove()
            selectedMarker = map.addMarker(
                MarkerOptions()
                    .position(selectedPoi!!.latLng)
                    .title(selectedPoi!!.name)
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation(googleMap: GoogleMap) {
        val locationResult = fusedLocationProviderClient.lastLocation
        locationResult.addOnCompleteListener(requireActivity()) { task ->
            if (task.isSuccessful) {
                // Set the map's camera position to the current location of the device.
                lastKnownLocation = task.result
                lastKnownLocation?.let {
                    googleMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                it.latitude,
                                it.longitude
                            ), DEFAULT_ZOOM.toFloat()
                        )
                    )

                }
            } else {
                googleMap.moveCamera(
                    CameraUpdateFactory
                        .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat())
                )
            }
        }
    }

    companion object {
        private const val ACCESS_FINE_LOCATION_REQUEST_CODE = 10002
        private const val DEFAULT_ZOOM = 15
    }
}
