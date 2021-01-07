package com.arfist.armona.screen.title

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.arfist.armona.R
import com.arfist.armona.databinding.TitleFragmentBinding

class TitleFragment : Fragment() {

    companion object {
        fun newInstance() = TitleFragment()
    }

    private lateinit var viewModel: TitleViewModel
    private lateinit var binding: TitleFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate vire and instance of binding
        binding = DataBindingUtil.inflate(inflater, R.layout.title_fragment, container, false)
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

}