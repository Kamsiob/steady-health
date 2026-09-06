package com.kamsiob.steadyhealth

import android.app.Application

/**
 * The application object, deliberately almost empty.
 *
 * Nothing is initialised here that a screen could initialise when it needs it.
 * Section C7 of the standards makes lazy loading a requirement for the model, and
 * the same reasoning applies to the database and everything else: an app that
 * opens an encrypted database before it knows whether this launch will read one
 * is slower at the only moment a person is watching.
 */
class SteadyApplication : Application()
