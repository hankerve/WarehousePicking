package com.topsmarteye.warehousepicking.stockList

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import com.topsmarteye.warehousepicking.R
import com.topsmarteye.warehousepicking.databinding.ActivityStockListBinding
import com.topsmarteye.warehousepicking.hideSystemUI
import com.topsmarteye.warehousepicking.network.ApiStatus
import com.topsmarteye.warehousepicking.network.networkServices.LoginService


class StockListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStockListBinding
    private lateinit var navController: NavController
    lateinit var stockListViewModel: StockListViewModel
//    private lateinit var gestureDetector: GestureDetector

    private var isStart: Boolean = true

    // This will mirror eventDisableControl in viewModel
    private var isControlDisabled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_stock_list)
        binding.lifecycleOwner = this

        navController = findNavController(R.id.stockListNavHostFragment)
        // The stockListViewModel will be shared between this activity and its fragments
        stockListViewModel = ViewModelProviders.of(this).get(StockListViewModel::class.java)
        stockListViewModel.setContext(this)
        binding.stockListViewModel = stockListViewModel

//        gestureDetector = GestureDetector(this, FlingGestureListener())

        setupListener()

    }

    override fun onResume() {
        super.onResume()
        // Enter the sticky immersive mode
        hideSystemUI()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isStart && keyCode == KeyEvent.KEYCODE_STAR) {
            stockListViewModel.onInputFragmentKeyboardScan()
            return true
        } else if (!isStart && !isControlDisabled) {
            when (keyCode) {
                KeyEvent.KEYCODE_1 -> {
                    stockListViewModel.onNext()
                    return true
                }
                KeyEvent.KEYCODE_2 -> {
                    stockListViewModel.onRestock()
                    return true
                }
                KeyEvent.KEYCODE_3 -> {
                    stockListViewModel.onOutOfStock()
                    return true
                }
                KeyEvent.KEYCODE_4 -> {
                    binding.resetOrderButton.requestFocus()
                    stockListViewModel.onResetOrder()
                    return true
                }
            }
        }

        return super.onKeyDown(keyCode, event)
    }

//    override fun onTouchEvent(event: MotionEvent?): Boolean {
//        gestureDetector.onTouchEvent(event)
//        return super.onTouchEvent(event)
//    }

    override fun onBackPressed() {
        binding.upButton.performClick()
    }

    private fun setupListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            changeNavItemsVisibility(destination)
        }

        binding.upButton.setOnClickListener {
            if (isStart) {
                finish()
            } else {
                navController.navigateUp()
                stockListViewModel.onNavigateToInput()
            }
        }

        stockListViewModel.currentIndex.observe(this, Observer {
            binding.currentIndexTextView.text =
                resources.getString(R.string.current_index_format, it + 1, stockListViewModel.totalItems.value)
        })

        stockListViewModel.eventDisableControl.observe(this, Observer {
            isControlDisabled = it
        })

        // Disable reset order button when loading
        stockListViewModel.listFragmentApiStatus.observe(this, Observer {
            when (it) {
                ApiStatus.LOADING -> binding.resetOrderButton.isClickable = false
                ApiStatus.NONE -> binding.resetOrderButton.isClickable = true
                else -> return@Observer
            }
        })

        // Finish the activity if user is not logged in
        LoginService.isLoggedIn.observe(this, Observer {
            if (!it) {
                finish()
            }
        })

    }

    private fun changeNavItemsVisibility(destination: NavDestination) {
        Log.d("destination", destination.id.toString())
        if (destination.id == navController.graph.startDestination) {
            binding.currentIndexTextView.visibility = View.GONE
            binding.resetOrderButton.visibility = View.GONE
            isStart = true
        } else {
            binding.currentIndexTextView.visibility = View.VISIBLE
            binding.resetOrderButton.visibility = View.VISIBLE
            isStart = false
        }
    }




}
