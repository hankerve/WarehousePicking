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
import androidx.navigation.fragment.navArgs
import com.topsmarteye.warehousepicking.OUT_OF_STOCK_DIALOG_REQUEST_CODE
import com.topsmarteye.warehousepicking.R
import com.topsmarteye.warehousepicking.RESET_ORDER_DIALOG_REQUEST_CODE
import com.topsmarteye.warehousepicking.RESTOCK_DIALOG_REQUEST_CODE
import com.topsmarteye.warehousepicking.dialog.RestockDialogActivity
import com.topsmarteye.warehousepicking.dialog.YesNoDialogActivity
import com.topsmarteye.warehousepicking.databinding.FragmentStockListBinding

class StockListFragment : Fragment() {

    private lateinit var binding: FragmentStockListBinding
    private lateinit var stockListViewModel: StockListViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_stock_list, container, false)
        binding.lifecycleOwner = this

        val inputFragmentArgs by navArgs<StockListFragmentArgs>()

        // Get shared viewModel
        stockListViewModel = activity?.run {
            ViewModelProviders.of(this).get(StockListViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        stockListViewModel.onDataLoaded(inputFragmentArgs.orderNumber)
        binding.stockListViewModel = stockListViewModel

        setViewModelObserver()

        return binding.root
    }

    private fun setViewModelObserver() {

        stockListViewModel.isLastItem.observe(this, Observer { isLastItem ->
            if (isLastItem) {
                binding.nextItemCardView.visibility = View.GONE
                binding.finishOrderButton.visibility = View.VISIBLE
                binding.nextButton.visibility = View.GONE

                binding.finishOrderButton.requestFocus()
            }
        })

        stockListViewModel.eventRestock.observe(this, Observer {
            if (it) {
                binding.needsRestockingButton.requestFocus()
                popRestock()
            }
        })

        stockListViewModel.eventOutOfStock.observe(this, Observer {
            if (it) {
                binding.outOfStockButton.requestFocus()
                popOutOfStock()
            }
        })

        stockListViewModel.eventResetOrder.observe(this, Observer {
            if (it) {
                popResetOrder()
            }
        })

        stockListViewModel.currentIndex.observe(this, Observer {
            // Whenever item changes, nextButton will have the focus
            binding.nextButton.requestFocus()
        })

        stockListViewModel.eventFinishOrder.observe(this, Observer {
            if (it && binding.finishOrderButton.visibility == View.VISIBLE) {
                binding.finishOrderButton.requestFocus()
                stockListViewModel.onFinishOrderComplete()
                activity?.finish()
            }
        })
    }

    private fun popRestock() {
        val intent = Intent(context, RestockDialogActivity::class.java)
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
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            RESTOCK_DIALOG_REQUEST_CODE -> stockListViewModel.onRestockComplete()
            OUT_OF_STOCK_DIALOG_REQUEST_CODE -> stockListViewModel.onOutOfStockComplete()
            RESET_ORDER_DIALOG_REQUEST_CODE -> stockListViewModel.onResetOrderComplete()
        }

        when (resultCode) {
            Activity.RESULT_CANCELED -> Toast.makeText(context, "cancelled", Toast.LENGTH_SHORT).show()
            Activity.RESULT_OK -> Toast.makeText(context, "confirmed", Toast.LENGTH_SHORT).show()
        }
    }

}