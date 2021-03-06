package com.topsmarteye.warehousepicking.stockList

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.zxing.integration.android.IntentIntegrator
import com.topsmarteye.warehousepicking.*
import com.topsmarteye.warehousepicking.dialog.RestockDialogActivity
import com.topsmarteye.warehousepicking.dialog.YesNoDialogActivity
import com.topsmarteye.warehousepicking.databinding.FragmentStockListBinding
import com.topsmarteye.warehousepicking.network.ApiStatus


class StockListFragment : Fragment() {

    private lateinit var binding: FragmentStockListBinding
    private lateinit var stockListViewModel: StockListViewModel
    private lateinit var integrator: IntentIntegrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get shared viewModel
        stockListViewModel = activity?.run {
            ViewModelProviders.of(this).get(StockListViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        setupBarcodeIntegrator()

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_stock_list, container, false)
        binding.lifecycleOwner = this

        binding.stockListViewModel = stockListViewModel

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        // Start marquee of textView when resume
        binding.nameTextView.isSelected = true
    }

    private fun setupBarcodeIntegrator() {
        integrator = IntentIntegrator.forSupportFragment(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES)
    }

    private fun setupListeners() {

        stockListViewModel.isLastItem.observe(viewLifecycleOwner, Observer { isLastItem ->
            if (isLastItem) {
                binding.nextItemCardView.visibility = View.GONE
            } else {
                binding.nextItemCardView.visibility = View.VISIBLE
            }
        })

        // Doesn't need to think about clickable, since keyboard will be disabled when appropriate
        stockListViewModel.eventNext.observe(viewLifecycleOwner, Observer {
            if (it) {
                binding.nextButton.requestFocus()
                integrator.setRequestCode(STOCK_LIST_NEXT_SCAN_REQUEST_CODE)
                integrator.setPrompt(getString(R.string.barcode_scan_prompt_format,
                    "\"" + stockListViewModel.currentItem.value!!.name + "\""))
                integrator.initiateScan()
            }
        })

        stockListViewModel.eventRestock.observe(viewLifecycleOwner, Observer {
            if (it) {
                binding.needsRestockingButton.requestFocus()
                integrator.setRequestCode(STOCK_LIST_RESTOCK_SCAN_REQUEST_CODE)
                integrator.setPrompt(getString(R.string.barcode_scan_prompt_format,
                    "\"" + stockListViewModel.currentItem.value!!.name + "\""))
                integrator.initiateScan()
            }
        })

        stockListViewModel.eventOutOfStock.observe(viewLifecycleOwner, Observer {
            if (it) {
                binding.outOfStockButton.requestFocus()
                popOutOfStock()
            }
        })

        stockListViewModel.eventResetOrder.observe(viewLifecycleOwner, Observer {
            if (it) {
                popResetOrder()
            }
        })

        stockListViewModel.currentIndex.observe(viewLifecycleOwner, Observer {
            // Whenever item changes, marquee for current item textView will start
            binding.nameTextView.isSelected = true
        })

        stockListViewModel.eventFinishActivity.observe(viewLifecycleOwner, Observer {
            if (it) {
                stockListViewModel.onFinishActivityComplete()
                activity?.finish()
            }
        })

        stockListViewModel.eventDisableControl.observe(viewLifecycleOwner, Observer {
            if (it) {
                disableControlButtons()
            } else {
                enableControlButtons()
            }
        })

        stockListViewModel.eventDateFormatError.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                popRetryDialog(getString(R.string.date_formatting_error_message), getString(R.string.ignore))
                stockListViewModel.onDateFormatErrorComplete()
            }
        })

        stockListViewModel.listFragmentApiStatus.observe(viewLifecycleOwner, Observer {
            when (it!!) {
                ApiStatus.LOADING -> {
                    startAnimation()
                    stockListViewModel.onDisableControl()
                }
                ApiStatus.ERROR -> {
                    popRetryDialog(getString(R.string.network_error_message), getString(R.string.retry))
                }
                ApiStatus.DONE -> {
                    return@Observer
                }
                ApiStatus.NONE -> {
                    stopAnimation()
                    stockListViewModel.onDisableControlComplete()
                }
            }
        })

        stockListViewModel.eventBarcodeConfirmError.observe(viewLifecycleOwner, Observer {
            if (it) {
                popRetryDialog(getString(R.string.barcode_confirm_error_message), getString(R.string.retry))
                // Don't care about the result, so complete here
                stockListViewModel.onBarcodeConfirmErrorComplete()
            }
        })

        stockListViewModel.eventOrderReloaded.observe(viewLifecycleOwner, Observer {
            if (it) {
                stockListViewModel.onOrderReloadedComplete()
            }
        })

        stockListViewModel.eventRestockItemsAdded.observe(viewLifecycleOwner, Observer {
            if (it) {
                stockListViewModel.onRestockItemsAddedComplete()
            }
        })

        stockListViewModel.eventMakeToast.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                stockListViewModel.onMakeToastComplete()
            }
        })
    }

    private fun disableControlButtons() {
        binding.nextButton.isEnabled = false
        binding.needsRestockingButton.isClickable = false
        binding.outOfStockButton.isClickable = false
    }

    private fun enableControlButtons() {
        binding.nextButton.isEnabled = true
        binding.needsRestockingButton.isClickable = true
        binding.outOfStockButton.isClickable = true
    }

    private fun startAnimation() {
        binding.submitProgressBar.visibility = View.VISIBLE
        binding.submitTextView.visibility = View.VISIBLE
    }

    private fun stopAnimation() {
        binding.submitProgressBar.visibility = View.GONE
        binding.submitTextView.visibility = View.GONE
    }

    private fun popRestock() {
        val intent = Intent(context, RestockDialogActivity::class.java).apply {
            putExtra("maxQuantity", stockListViewModel.currentItem.value!!.quantity)
        }
        startActivityForResult(intent, RESTOCK_DIALOG_REQUEST_CODE)
    }

    private fun popOutOfStock() {
        val intent = Intent(context, YesNoDialogActivity::class.java).apply {
            putExtra("dialogTitle", getString(R.string.confirm_out_of_stock))
        }
        startActivityForResult(intent, OUT_OF_STOCK_DIALOG_REQUEST_CODE)
    }

    private fun popResetOrder() {
        val intent = Intent(context, YesNoDialogActivity::class.java).apply {
            putExtra("dialogTitle", getString(R.string.confirm_reset_order))
        }
        startActivityForResult(intent, RESET_ORDER_DIALOG_REQUEST_CODE)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            STOCK_LIST_NEXT_SCAN_REQUEST_CODE -> {
                // If scan activity is cancelled
                if (resultCode == Activity.RESULT_CANCELED) {
                    stockListViewModel.onNextComplete(null)
                    return
                }
                val result = IntentIntegrator.parseActivityResult(resultCode, data)
                stockListViewModel.onNextComplete(result)
            }
            STOCK_LIST_RESTOCK_SCAN_REQUEST_CODE -> {
                // If scan activity is cancelled
                if (resultCode == Activity.RESULT_CANCELED) {
                    stockListViewModel.onRestockComplete(null)
                    return
                }
                val result = IntentIntegrator.parseActivityResult(resultCode, data)
                if (stockListViewModel.confirmBarcodeFromScanResult(result)) {
                    popRestock()
                } else {
                    stockListViewModel.onBarcodeConfirmError()
                    stockListViewModel.onRestockComplete(null)
                }
            }

            RESTOCK_DIALOG_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    val quantity = data!!.extras!!["restockQuantity"] as Int
                    stockListViewModel.onRestockComplete(quantity)
                } else {
                    stockListViewModel.onRestockComplete(null)
                }
            }
            OUT_OF_STOCK_DIALOG_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    stockListViewModel.onOutOfStockComplete(false)
                } else {
                    stockListViewModel.onOutOfStockComplete(true)
                }
            }
            RESET_ORDER_DIALOG_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    stockListViewModel.onResetOrderComplete(false)
                } else {
                    stockListViewModel.onResetOrderComplete(true)
                }
            }
            else -> {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

}