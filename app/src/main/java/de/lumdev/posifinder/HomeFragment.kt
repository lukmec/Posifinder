package de.lumdev.posifinder

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.lumdev.posifinder.databinding.FragmentHomeBinding

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {

    var lat: Double = 0.0
    var lon:Double = 0.0

    private var _binding: FragmentHomeBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

//    private lateinit var fusedLocationClient: FusedLocationProviderClient


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        activity?.findViewById<FloatingActionButton>(R.id.fab)?.show(); //show FAB on screen

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        readPreferencesAndUpdateViews()

        val sharedPref = activity?.getSharedPreferences(getString(R.string.pref_file), Context.MODE_PRIVATE)

        binding.setButton.setOnClickListener{
            if (sharedPref != null) {
                //reset location
                lat = 0.0
                lon = 0.0
                val titleText = "Click + to fetch current position."
                //save resetted location to preferences
                with (sharedPref.edit()) {
                    putDouble(getString(R.string.pref_id_latitude), lat)
                    putDouble(getString(R.string.pref_id_longitude), lon)
                    putString(getString(R.string.pref_id_title), titleText)
                    apply()
                //display resetted location
                binding.latitudeTextView.text = lat.toString()
                binding.longitudeTextView.text = lon.toString()
                binding.titleTextView.text = titleText
                }
            }else{
                Toast.makeText(context, "Error. Can not save resetted location.", Toast.LENGTH_SHORT).show()
            }


        }

        binding.showButton.setOnClickListener{
            //show Coordinates on Map
            if (sharedPref != null){
//            println("Fragment Preferences != null")
                lat = sharedPref.getDouble(getString(R.string.pref_id_latitude), 3002.0)
                lon = sharedPref.getDouble(getString(R.string.pref_id_longitude), 4003.0)

                //create Intent to show Geo-Location and Marker in appropriate App
                val mapIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("geo:$lat,$lon?q=$lat,$lon(Your Position)")
                )
                startActivity(mapIntent)
            }else{
                println("Fragment Preferences = null")
                Toast.makeText(context, "Error. Can read saved location.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonFetchMethodGetLast.setOnClickListener {
            val newFetchMethod = getString(R.string.btn_fetch_method_getLast)
            fetchMethodOnClickListener(newFetchMethod, sharedPref)
        }
        binding.buttonFetchMethodGetCurrent.setOnClickListener {
            val newFetchMethod = getString(R.string.btn_fetch_method_getCurrent)
            fetchMethodOnClickListener(newFetchMethod, sharedPref)
        }
        binding.buttonFetchMethodRequestUpdates.setOnClickListener {
            val newFetchMethod = getString(R.string.btn_fetch_method_request_updates)
            fetchMethodOnClickListener(newFetchMethod, sharedPref)
        }
    }

    private fun fetchMethodOnClickListener(newFetchMethod: String, sharedPref: SharedPreferences?){
        //setting global variable to new fetch method
        fetchMethod = newFetchMethod
        fetchType = when (fetchMethod){
            getString(R.string.btn_fetch_method_getLast) -> LocationFetchType.GET_LAST_LOCATION
            getString(R.string.btn_fetch_method_getCurrent) -> LocationFetchType.GET_CURRENT_LOCATION
            getString(R.string.btn_fetch_method_request_updates) -> LocationFetchType.REQUEST_LOCATION_UPDATES
            else -> LocationFetchType.NONE
        }
        //save new fetch method to preferences
        if (sharedPref != null){
            with (sharedPref.edit()) {
                putString(getString(de.lumdev.posifinder.R.string.pref_id_fetch_method),newFetchMethod)
                apply()
            }
            //set UI to new selected fetch method
            binding.fetchMethodTextView.text = newFetchMethod
        }
    }

    override fun onResume() {
        super.onResume()

        activity?.findViewById<FloatingActionButton>(R.id.fab)?.show(); //show FAB on screen

        readPreferencesAndUpdateViews()

    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }


    //from https://stackoverflow.com/questions/16319237/cant-put-double-sharedpreferences
    fun SharedPreferences.Editor.putDouble(key: String, double: Double) =
        putLong(key, java.lang.Double.doubleToRawLongBits(double))

    fun SharedPreferences.getDouble(key: String, default: Double) =
        java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(default)))



    fun readPreferencesAndUpdateViews(){
        val sharedPref = activity?.getSharedPreferences(getString(R.string.pref_file), Context.MODE_PRIVATE)
        var titleText:String? = null
        val last_used_posifinder: String?
        val last_used_save_posi: String?
        val last_used_save_posi_2: String?
        val fetchMethod: String?
        //for restoring saved location-values from preferences
        if (sharedPref != null){
//            println("Fragment Preferences != null")
            lat = sharedPref.getDouble(getString(R.string.pref_id_latitude), 3002.0)
            lon = sharedPref.getDouble(getString(R.string.pref_id_longitude), 4003.0)
            titleText = sharedPref.getString(getString(R.string.pref_id_title), "Position on 10.12.2023 (07:19)")
            last_used_posifinder = sharedPref.getString(getString(R.string.pref_id_last_used_posifinder), "n.a.")
            last_used_save_posi = sharedPref.getString(getString(R.string.pref_id_last_used_save_position), "n.a.")
            last_used_save_posi_2 = sharedPref.getString(getString(R.string.pref_id_last_used_save_position_2), "n.a.")
            fetchMethod = sharedPref.getString(getString(R.string.pref_id_fetch_method), FETCH_METHOD_DEFAULT)
            //display stored location values
            binding.latitudeTextView.text = lat.toString()
            binding.longitudeTextView.text = lon.toString()
            binding.titleTextView.text = titleText
            binding.lastUsedPosiFinderTextView.text = last_used_posifinder
            binding.lastUsedSavePosiTextView.text = last_used_save_posi
            binding.lastUsedSavePosi2TextView.text = last_used_save_posi_2
            binding.fetchMethodTextView.text = fetchMethod

        }else{
            println("Fragment Preferences = null")
            Toast.makeText(context, "Error. Cant read saved location.", Toast.LENGTH_SHORT).show()
//            lat = 37.4219983
//            lon = -122.084
        }
    }

}