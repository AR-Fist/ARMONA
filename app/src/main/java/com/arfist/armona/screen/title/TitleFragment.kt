package com.arfist.armona.screen.title

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.arfist.armona.R
import com.arfist.armona.databinding.TitleFragmentBinding
import com.arfist.armona.screen.map.MapViewModel
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import timber.log.Timber

class TitleFragment : Fragment() {

//    companion object {
//        fun newInstance() = TitleFragment()
//    }

    private lateinit var viewModel: TitleViewModel
    private val mapViewModel: MapViewModel by activityViewModels()
    private lateinit var binding: TitleFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate view and instance of binding
        binding = DataBindingUtil.inflate(inflater, R.layout.title_fragment, container, false)
        initAutoCompleteFragment()
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(TitleViewModel::class.java)

        // Set listener
        binding.titleViewModel = viewModel

        binding.buttonAr.setOnClickListener { view : View ->
            view.findNavController().navigate(TitleFragmentDirections.actionTitleFragmentToArFragment())
        }
        binding.buttonMap.setOnClickListener { view: View ->
            view.findNavController().navigate(TitleFragmentDirections.actionTitleFragmentToMapFragment())
        }

    }

    private fun initAutoCompleteFragment() {
        // Initialize the AutocompleteSupportFragment.
        val autocompleteFragment =
            childFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                    as AutocompleteSupportFragment

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME))

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Timber.i(place.name)
                mapViewModel.getDirection(place.name!!)
            }

            override fun onError(status: Status) {
                Timber.e(status.statusMessage)
            }
        })
    }

}