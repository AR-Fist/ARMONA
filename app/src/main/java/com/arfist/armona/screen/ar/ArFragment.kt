package com.arfist.armona.screen.ar

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
import com.arfist.armona.databinding.ArFragmentBinding
import com.arfist.armona.screen.map.MapViewModel

class ArFragment : Fragment() {

    private lateinit var viewModel: ArViewModel
    private val mapViewModel: MapViewModel by activityViewModels()
    private lateinit var binding: ArFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.ar_fragment, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ArViewModel::class.java)

        binding.arViewModel = viewModel
        binding.buttonArMap.setOnClickListener { view: View ->
            view.findNavController().navigate(ArFragmentDirections.actionArFragmentToMapFragment())
        }
    }

}