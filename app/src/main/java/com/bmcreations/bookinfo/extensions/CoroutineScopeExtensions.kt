package com.bmcreations.bookinfo.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

private val job = Job()
internal val uiScope  = CoroutineScope(Dispatchers.Main + job)