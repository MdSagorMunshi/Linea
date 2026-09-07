package com.ryanshelby.linea.telecom

import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LineaConnectionService : ConnectionService() {

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        val connection = object : Connection() {
            override fun onShowIncomingCallUi() {
                super.onShowIncomingCallUi()
            }

            override fun onAnswer() {
                setActive()
            }

            override fun onReject() {
                setDisconnected(android.telecom.DisconnectCause(android.telecom.DisconnectCause.REJECTED))
                destroy()
            }

            override fun onDisconnect() {
                setDisconnected(android.telecom.DisconnectCause(android.telecom.DisconnectCause.LOCAL))
                destroy()
            }
        }
        connection.setInitializing()
        connection.connectionCapabilities = Connection.CAPABILITY_SUPPORT_HOLD or Connection.CAPABILITY_MUTE
        connection.setAddress(request?.address, TelecomManager.PRESENTATION_ALLOWED)
        connection.setActive()
        return connection
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        val connection = object : Connection() {
            override fun onAnswer() {
                setActive()
            }

            override fun onReject() {
                setDisconnected(android.telecom.DisconnectCause(android.telecom.DisconnectCause.REJECTED))
                destroy()
            }

            override fun onDisconnect() {
                setDisconnected(android.telecom.DisconnectCause(android.telecom.DisconnectCause.LOCAL))
                destroy()
            }
        }
        connection.setInitializing()
        connection.setAddress(request?.address, TelecomManager.PRESENTATION_ALLOWED)
        connection.setRinging()
        return connection
    }
}
