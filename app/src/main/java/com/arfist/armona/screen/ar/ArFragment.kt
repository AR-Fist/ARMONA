package com.arfist.armona.screen.ar

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.arfist.armona.R

class ArFragment : Fragment() {

    companion object {
        fun newInstance() = ArFragment()
    }

    private lateinit var viewModel: ArViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.ar_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ArViewModel::class.java)
        // TODO: Use the ViewModel
    }

}