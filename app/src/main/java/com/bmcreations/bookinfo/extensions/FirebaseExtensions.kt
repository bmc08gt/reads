package com.bmcreations.bookinfo.extensions

import com.google.firebase.auth.FirebaseAuth

val currentUser = FirebaseAuth.getInstance().currentUser
val isSignedIn = currentUser != null