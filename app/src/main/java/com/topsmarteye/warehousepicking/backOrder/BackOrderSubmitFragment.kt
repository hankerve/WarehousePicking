package com.topsmarteye.warehousepicking.backOrder

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.topsmarteye.warehousepicking.R
import com.topsmarteye.warehousepicking.databinding.FragmentBackOrderSubmitBinding
import com.topsmarteye.warehousepicking.network.ApiStatus
import com.topsmarteye.warehousepicking.popRetryDialog


class BackOrderSubmitFragment : Fragment() {

    private lateinit var binding: FragmentBackOrderSubmitBinding
    private lateinit var viewModel: BackOrderViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this).get(BackOrderViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_back_order_submit, container, false)
        binding.backOrderViewModel = viewModel
        binding.lifecycleOwner = this

        binding.restockQuantityEditText.hint = getString(R.string.maximum_quantity_format,
            viewModel.itemToRestock.value!!.quantity)

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.restockQuantityEditText.requestFocus()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        // start Item marquee
        binding.itemNameTextView.isSelected = true
    }

    private fun setupListeners() {
        binding.submitButton.setOnClickListener {
            try {
                if (binding.restockQuantityEditText.text.isNullOrEmpty()) {
                    throw Exception("Quantity is empty")
                }

                val quantity = binding.restockQuantityEditText.text.toString().toInt()
                if (quantity <= 0 ||
                    quantity > viewModel.itemToRestock.value!!.quantity)
                    throw Exception("Quantity is larger than possible quantity")

                viewModel.onSubmit(quantity)

            } catch (e: Exception) {
                Log.d("BackOrderSubmitFragment", "submitButton listener error ${e.message}")
                viewModel.onSubmitQuantityError()
            }
        }

        viewModel.submitApiStatus.observe(viewLifecycleOwner, Observer { status ->
            when(status) {
                ApiStatus.LOADING -> {
                    binding.progressBar.visibility = View.VISIBLE
                    viewModel.onDisableInteraction()
                }
                ApiStatus.DONE -> {
                    Toast.makeText(context, getString(R.string.back_order_success_toast), Toast.LENGTH_SHORT).show()
                }
                ApiStatus.ERROR -> {
                    popRetryDialog(getString(R.string.restock_network_error_message), getString(R.string.retry))
                }
                ApiStatus.NONE -> {
                    binding.progressBar.visibility = View.GONE
                    viewModel.onDisableInteractionComplete()
                }
                else -> return@Observer
            }
        })

        viewModel.eventDisableInteraction.observe(viewLifecycleOwner, Observer {
            if (it)
                disableInteraction()
            else
                enableInteraction()
        })

        viewModel.eventSubmitQuantityError.observe(viewLifecycleOwner, Observer {
            if (it) {
                popRetryDialog(getString(R.string.restock_quantity_error), getString(R.string.retry))
                viewModel.onSubmitQuantityErrorComplete()
            }
        })

        viewModel.eventNavigateToInput.observe(viewLifecycleOwner, Observer {
            if (it) {
                findNavController().navigateUp()
            }
        })
    }

    private fun disableInteraction() {
        binding.restockQuantityEditText.isEnabled = false
        binding.submitButton.isEnabled = false
    }

    private fun enableInteraction() {
        binding.restockQuantityEditText.isEnabled = true
        binding.submitButton.isEnabled = true
    }

}