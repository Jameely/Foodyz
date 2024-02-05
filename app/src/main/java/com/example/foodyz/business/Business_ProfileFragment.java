package com.example.foodyz.business;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.foodyz.R;
import com.example.foodyz.StartActivity;
import com.example.foodyz.personal.Personal_MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Business_ProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Business_ProfileFragment extends Fragment {

    private FirebaseAuth auth;

    public Business_ProfileFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment Business_ProfileFragment.
     */
    public static Business_ProfileFragment newInstance(String param1, String param2) {
        return new Business_ProfileFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_business_profile, container, false);

        EditText username = view.findViewById(R.id.username);
        EditText city = view.findViewById(R.id.address_city);
        EditText street = view.findViewById(R.id.address_street);
        EditText home_number = view.findViewById(R.id.address_number);
        EditText bank_number = view.findViewById(R.id.bank_number);
        EditText bank_branch = view.findViewById(R.id.bank_branch);

        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        String user_id = user.getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference users_collection = db.collection("business-accounts");

        // Write current profile settings onto text fields:
        users_collection.whereEqualTo("business-id", user_id)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        try {
                            QuerySnapshot querySnapshot = task.getResult();

                            if (querySnapshot != null && querySnapshot.size() == 1) {
                                // Document found, retrieve the value of all settings:

                                DocumentSnapshot document = querySnapshot.getDocuments().get(0);

                                username.setText(document.getString("user-name"));

                                Map<String, Object> address = (Map<String, Object>) document.get("address");
                                city.setText((String) address.get("city"));
                                street.setText((String) address.get("street"));
                                home_number.setText((String) address.get("number"));

                                Map<String, Object> card = (Map<String, Object>) document.get("bank-account");

                                bank_number.setText((String) card.get("number"));
                                bank_branch.setText((String) card.get("bank"));

                            } else {
                                // Either no document found or multiple matching documents were found. Direct user to start screen:
                                Log.e("FirestoreQuery", "Could not find ID of currently authenticated user. (ID: " + user_id + ")");

                                Toast.makeText(getContext(), "Multiple Users found!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(getActivity(), StartActivity.class));
                                getActivity().finish();
                            }
                        } catch (Exception e) {
                            // Exception found. Direct user to start screen:
                            Log.e("FirestoreQuery", "Error getting documents: " + e.getMessage());

                            Toast.makeText(getContext(), "Error!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getActivity(), StartActivity.class));
                            getActivity().finish();
                        }
                    } else {
                        // Exception found. Direct user to start screen:
                        Log.e("FirestoreQuery", "Error getting documents: " + task.getException().getMessage());

                        Toast.makeText(getActivity(), "Login Failed!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(getActivity(), StartActivity.class));
                        getActivity().finish();
                    }
                });

        Button save = view.findViewById(R.id.save_settings);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String txt_username = username.getText().toString();
                String txt_city = city.getText().toString();
                String txt_street = street.getText().toString();
                String txt_home_number = home_number.getText().toString();
                String txt_bank_number = bank_number.getText().toString();
                String txt_bank_branch = bank_branch.getText().toString();

                if (validateUserSettings(txt_username,
                        txt_city, txt_street, txt_home_number,
                        txt_bank_number, txt_bank_branch)) {

                    updateUser(txt_username,
                            txt_city, txt_street, txt_home_number,
                            txt_bank_number, txt_bank_branch);

                    Toast. makeText(getContext(), "Update successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getContext(), Business_MainActivity.class));
                    getActivity().finish();
                }
            }
        });

        return view;
    }

    private boolean validateUserSettings(String username,
                                         String city, String street, String home_number,
                                         String bank_number, String bank_branch) {

        // Check for empty fields:
        if(username.isEmpty() ||
                city.isEmpty() || street.isEmpty() || home_number.isEmpty() ||
                bank_branch.isEmpty() || bank_number.isEmpty()) {

            Toast. makeText(getContext(), "Field empty", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate bank details length:
        if (bank_branch.length() != 3 || bank_number.length() != 6) {
            Toast.makeText(getContext(), "Invalid credit card credentials!", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Make sure bank details are numbers:
        if (!bank_branch.matches("\\d+") || !bank_number.matches("\\d+")) {
            Toast. makeText(getContext(), "Invalid credit card number!", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void updateUser(String username,
                            String city, String street, String home_number,
                            String bank_number, String bank_branch) {

        FirebaseUser user = auth.getCurrentUser();
        String user_id = user.getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Update 'personal-accounts' collection
        db.collection("business-accounts")
                .whereEqualTo("business-id", user_id)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // Update the document with the new values
                        document.getReference().update(
                                "user-name", username,
                                "address.city", city,
                                "address.street", street,
                                "address.number", home_number,
                                "bank-account.bank", bank_branch,
                                "credit-card.number", bank_number
                        ).addOnSuccessListener(aVoid -> {
                            // Document updated successfully
                            Log.d("Firestore", "DocumentSnapshot updated successfully");
                        }).addOnFailureListener(e -> {
                            // Handle errors
                            Log.e("Firestore", "Error updating document", e);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle errors
                    Log.e("Firestore", "Error getting documents", e);
                });
    }
}