# Warehouse Picking
## Aug, 2019

Runs on Android Smart glasses free warehouse pickers' hands. Written with Kotlin and AndroidX.

The app contains two components, the "Picking" option let users fetch all items beloging to an order, item status can be dynamic and can be updated during runtime (when other pickers are doing the same order). The "Back Order" option let user record back orders of an item if an item is out of stock.

Since typing on smart glasses are virtually "impossible" and would create a bad user experience, most of the app's input is done using QRCode scanning or otherwise 1-dimension swipe and tap.

Picking Items:

<img src="img/1.gif" width="400">

Home Screen:

<img src="img/warehouse-main.png" width="400">

The app showing list of items waiting to be picked, list update each time when an item is picked:

<img src="img/warehouse-pick.png" width="400">

Customized QRCode login screen:

<img src="img/warehouse-scan.png" width="400">
