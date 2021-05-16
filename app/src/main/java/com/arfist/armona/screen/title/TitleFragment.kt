package com.arfist.armona.screen.title

import android.content.pm.PackageManager
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.arfist.armona.MainActivity.Companion.PERMISSION_REQUEST_MAP
import com.arfist.armona.MainActivity.Companion.permissionList
import com.arfist.armona.R
import com.arfist.armona.databinding.TitleFragmentBinding
import com.arfist.armona.utils.hasPermission
import com.arfist.armona.shared.SharedViewModel
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import timber.log.Timber

class TitleFragment : Fragment() {

    private lateinit var viewModel: TitleViewModel
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var binding: TitleFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.i("onCreateView")

        // Inflate view and instance of binding
        binding = DataBindingUtil.inflate(inflater, R.layout.title_fragment, container, false)
        initAutoCompleteFragment()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.i("onViewCreated")

        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(TitleViewModel::class.java)

        // Set listener
        binding.titleViewModel = viewModel
        binding.sharedViewModel = sharedViewModel

        binding.buttonAr.setOnClickListener { mView : View ->
            mView.findNavController().navigate(TitleFragmentDirections.actionTitleFragmentToArFragment())
        }
        binding.buttonMap.setOnClickListener { mView: View ->
            mView.findNavController().navigate(TitleFragmentDirections.actionTitleFragmentToMapFragment())
        }
        binding.lifecycleOwner = viewLifecycleOwner

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Timber.i("onActivityCreated")
        super.onActivityCreated(savedInstanceState)
    }

    private fun initAutoCompleteFragment() {
        Timber.i("init auto complete")

        // Initialize the AutocompleteSupportFragment.
        val autocompleteFragment =
            childFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                    as AutocompleteSupportFragment

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME))
//        autocompleteFragment.setCountry("Thailand")

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Timber.i(place.name)
                sharedViewModel.setDestination(place.name!!)
            }

            override fun onError(status: Status) {
                Timber.e(status.statusMessage)
            }
        })
    }

}