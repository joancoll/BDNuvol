package cat.dam.andy.bdnuvol;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity {
    //Membres de classe
    //Nom de la base de dades (Firebase)
    private final String COLLECTION_KEY = "USERS";
    //classes pròpies i variables globals
    private final String TAG = "FirebaseFireStore";
    private User user;
    private final ArrayList<User> userList = new ArrayList<>();
    private String message = "";
    //referències a la base de dades
    private FirebaseFirestore db;
    private CollectionReference colRef;
    //referències a la vista
    private TextView tv_message;
    private EditText et_name, et_lastname;
    private LinearLayout ll_deleteUsers;
    private Button btn_addUser, btn_deleteUser;
    private Spinner sp_users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initDatabase();
        initListeners();
    }

    private void initViews() {
        sp_users = findViewById(R.id.sp_users);
        btn_addUser = findViewById(R.id.btn_addUser);
        btn_deleteUser = findViewById(R.id.btn_deleteUser);
        et_name = findViewById(R.id.et_name);
        et_lastname = findViewById(R.id.et_lastname);
        tv_message = findViewById(R.id.tv_message);
        ll_deleteUsers = findViewById(R.id.ll_userDeletion);
        //per fer que la primera lletra a l'entrar sigui majuscula
        et_name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        et_lastname.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
    }

    private void initDatabase() {
        //inicialitzem la base de dades
        db = FirebaseFirestore.getInstance();
        //afegim un esdeveniment per escoltar qualsevol canvi de la col·lecció Usuaris i actualitzar spinner
        db.collection(COLLECTION_KEY).addSnapshotListener((documentSnapshots, e) -> {
            //en cas de qualsevol canvi llegirem tots els valors i els posarem a una llista
            userList.clear();
            assert documentSnapshots != null;
            for (DocumentSnapshot snapshot : documentSnapshots) {
                //afegim en un ArrayList que conté objectes User l'objecte
                userList.add(new User(snapshot.getString("name"), snapshot.getString("lastname")));
            }
            // Si API>24 i Java 8 => userList.sort(String::compareToIgnoreCase);i
            Collections.sort(userList, new UserComparator());//ordena segons un comparador propi
            //en cas de que no hi hagi usuaris amaguem el control
            ll_deleteUsers.setVisibility((userList.size() > 0) ? View.VISIBLE : View.INVISIBLE);
            ArrayAdapter<User> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, userList);
            adapter.notifyDataSetChanged();
            sp_users.setAdapter(adapter);//enviem dades adaptador al component spinner
        });
    }

    private void initListeners() {

        btn_addUser.setOnClickListener(v -> {
            final String name = et_name.getText().toString();
            final String lastname = et_lastname.getText().toString();
            if (isValidEntry(name, lastname)) {
                //si cap entrada no és buida comprovem si ja existeix el nom
                db.collection(COLLECTION_KEY)
                        .whereEqualTo("name", name)
                        .whereEqualTo("lastname", lastname)
                        .get().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                QuerySnapshot querySnapShot = task.getResult();
                                if (querySnapShot.size() > 0) {
                                    //si existeix algun usuari amb el mateix nom no el guardem
                                    message = getString(R.string.error_user_already_exists) + " " + name + " " + lastname;
                                    Log.d(TAG, message);
                                    showInfo(message);
                                } else { //si no existeix cap usuari amb el mateix nom el guardem
                                    user = new User(name, lastname);
                                    db.collection(COLLECTION_KEY)
                                            .add(user)
                                            .addOnSuccessListener(documentReference -> {
                                                message = getString(R.string.new_user_added) + " " + user.toString();
                                                Log.d(TAG, message);
                                                showInfo(message);
                                                et_name.setText("");
                                                et_lastname.setText("");
                                            })
                                            .addOnFailureListener(e -> {
                                                message = getString(R.string.error_user_add) + " " + e;
                                                Log.w(TAG, message);
                                                showInfo(message);
                                            });
                                }
                            } else {
                                message = getString(R.string.error_cannot_test_user) + "(" + task.getException() + ")";
                                Log.w(TAG, message);
                                showInfo(message);
                            }
                        });
            } else {
                message = getString(R.string.error_empty_fields);
                Log.w(TAG, message);
                showInfo(message);
            }
        });

        btn_deleteUser.setOnClickListener(v -> {
            if (sp_users.getSelectedItem() != null) {
                int i = sp_users.getSelectedItemPosition();
                user = userList.get(i);
                colRef = db.collection(COLLECTION_KEY);
                Query query = colRef.whereEqualTo("name", user.getName()).whereEqualTo("lastname", user.getLastname());
                query.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot document : task.getResult()) {
                            colRef.document(document.getId()).delete();
                            message = getString(R.string.user_deletion) + " " + user.toString();
                            Log.d(TAG, message);
                            showInfo(message);
                        }
                    } else {
                        message = getString(R.string.error_user_deletion) + " (" + task.getException() + ")";
                        Log.w(TAG, message);
                        showInfo(message);
                    }
                });
            } else {
                message = getString(R.string.error_no_selected_user);
                Log.w(TAG, message);
                showInfo(message);
            }
        });
    }

    static class UserComparator implements Comparator<Object> {
        //Ordena els usuaris pel nom ignorant les majúscules
        @Override
        public int compare(Object o1, Object o2) {
            String name1 = ((User) o1).getName();
            String name2 = ((User) o2).getName();
            return (name1.compareToIgnoreCase(name2));
        }
    }

    private boolean isValidEntry(String name, String lastname) {
        if (name.length() == 0 || lastname.length() == 0) {
            message = getString(R.string.error_empty_fields);
            Log.w(TAG, message);
            showInfo(message);
            return false;
        } else {
            return true;
        }
    }

    public void showInfo(String message) {
        tv_message = findViewById(R.id.tv_message);
        tv_message.setText(message);
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
    }
}
