package morales.jesus.practica10

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CustomCredential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import favela.luis.practica10.Bienvenida
import favela.luis.practica10.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    object Global {
        var preferencias_compartidas = "sharedpreferences"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        verificar_sesion_abierta()
    }

    private fun verificar_sesion_abierta() {
        val sesion = getSharedPreferences(Global.preferencias_compartidas, Context.MODE_PRIVATE)
        val correo = sesion.getString("Correo", null)
        val proveedor = sesion.getString("Proveedor", null)
        if (correo != null && proveedor != null) {
            val intent = Intent(applicationContext, Bienvenida::class.java)
            intent.putExtra("Correo", correo)
            intent.putExtra("Proveedor", proveedor)
            startActivity(intent)
            finish()
        }
    }

    private fun guardar_sesion(correo: String, proveedor: String) {
        val editor = getSharedPreferences(Global.preferencias_compartidas, Context.MODE_PRIVATE).edit()
        editor.putString("Correo", correo)
        editor.putString("Proveedor", proveedor)
        editor.apply() // Solo uno, más eficiente que commit
    }

    fun login_firebase(correo: String, pass: String) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(correo, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val intent = Intent(applicationContext, Bienvenida::class.java)
                    intent.putExtra("Correo", task.result.user?.email)
                    intent.putExtra("Proveedor", "Usuario/Contraseña")
                    startActivity(intent)
                    guardar_sesion(task.result.user?.email.toString(), "Usuario/Contraseña")
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Usuario/Contraseña incorrecto(s)",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    fun loginGoogle() {
        val context = this
        val credentialManager = CredentialManager.create(context)

        val signInWithGoogleOption: GetSignInWithGoogleOption =
            GetSignInWithGoogleOption.Builder(getString(R.string.Jesus))
                .setNonce("nonce")
                .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )
                handleSignIn(result)
            } catch (e: GetCredentialException) {
                Toast.makeText(
                    context,
                    "Error al obtener la credencial: $e",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun handleSignIn(result: GetCredentialResponse) {
        val credential = result.credential
        when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val credencial = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

                        FirebaseAuth.getInstance().signInWithCredential(credencial)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    val intent = Intent(applicationContext, Bienvenida::class.java)
                                    intent.putExtra("Correo", task.result.user?.email)
                                    intent.putExtra("Proveedor", "Google")
                                    startActivity(intent)
                                    guardar_sesion(task.result.user?.email.toString(), "Google")
                                    finish()
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Error en la autenticación con Firebase",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                    } catch (e: GoogleIdTokenParsingException) {
                        Toast.makeText(
                            this,
                            "Token de Google inválido",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Tipo de credencial inesperado",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            else -> {
                Toast.makeText(
                    this,
                    "Credencial desconocida",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
