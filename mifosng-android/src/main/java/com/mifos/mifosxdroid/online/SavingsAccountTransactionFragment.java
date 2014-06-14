package com.mifos.mifosxdroid.online;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.jakewharton.fliptables.FlipTable;
import com.mifos.mifosxdroid.R;
import com.mifos.objects.PaymentTypeOption;
import com.mifos.objects.accounts.savings.SavingsAccount;
import com.mifos.objects.accounts.savings.SavingsAccountTransactionRequest;
import com.mifos.objects.accounts.savings.SavingsAccountTransactionResponse;
import com.mifos.objects.accounts.savings.SavingsAccountWithAssociations;
import com.mifos.objects.templates.savings.SavingsAccountTransactionTemplate;
import com.mifos.services.API;
import com.mifos.utils.Constants;
import com.mifos.utils.DateHelper;
import com.mifos.utils.SafeUIBlockingUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class SavingsAccountTransactionFragment extends Fragment {


    @InjectView(R.id.tv_clientName) TextView tv_clientName;
    @InjectView(R.id.tv_savingsAccountNumber) TextView tv_accountNumber;
    @InjectView(R.id.et_transaction_date) EditText et_transactionDate;
    @InjectView(R.id.et_transaction_amount) EditText et_transactionAmount;
    @InjectView(R.id.sp_payment_type) Spinner sp_paymentType;
    @InjectView(R.id.bt_reviewTransaction) Button bt_reviewTransaction;
    @InjectView(R.id.bt_cancelTransaction) Button bt_cancelTransaction;


    View rootView;

    SafeUIBlockingUtility safeUIBlockingUtility;

    private OnFragmentInteractionListener mListener;

    ActionBarActivity activity;

    ActionBar actionBar;

    SharedPreferences sharedPreferences;

    String savingsAccountNumber;
    String transactionType;     //Defines if the Transaction is a Deposit to an Account or a Withdrawal from an Account
    String clientName;

    SavingsAccount savingsAccountWithAssociations;

    // Values to be fetched from Savings Account Template
    List<PaymentTypeOption> paymentTypeOptionList;
    HashMap<String, Integer> paymentTypeHashMap = new HashMap<String, Integer>();

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param savingsAccountWithAssociations Savings Account of the Client with some additional association details
     * @param transactionType Type of Transaction (Deposit or Withdrawal)
     *
     * @return A new instance of fragment SavingsAccountTransactionDialogFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SavingsAccountTransactionFragment newInstance(SavingsAccountWithAssociations savingsAccountWithAssociations, String transactionType) {
        SavingsAccountTransactionFragment fragment = new SavingsAccountTransactionFragment();
        Bundle args = new Bundle();
        args.putString(Constants.SAVINGS_ACCOUNT_NUMBER, savingsAccountWithAssociations.getAccountNo());
        args.putString(Constants.SAVINGS_ACCOUNT_TRANSACTION_TYPE, transactionType);
        args.putString(Constants.CLIENT_NAME, savingsAccountWithAssociations.getClientName());
        fragment.setArguments(args);
        return fragment;
    }

    public SavingsAccountTransactionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

            savingsAccountNumber = getArguments().getString(Constants.SAVINGS_ACCOUNT_NUMBER);
            transactionType = getArguments().getString(Constants.SAVINGS_ACCOUNT_TRANSACTION_TYPE);
            clientName = getArguments().getString(Constants.CLIENT_NAME);

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        rootView = inflater.inflate(R.layout.fragment_savings_account_transaction, container, false);

        activity = (ActionBarActivity) getActivity();

        safeUIBlockingUtility = new SafeUIBlockingUtility(getActivity());

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);

        actionBar = activity.getSupportActionBar();

        if(transactionType.equals(Constants.SAVINGS_ACCOUNT_TRANSACTION_DEPOSIT))
            actionBar.setTitle("Savings Account - Deposit");
        else if(transactionType.equals(Constants.SAVINGS_ACCOUNT_TRANSACTION_WITHDRAWAL))
            actionBar.setTitle("Savings Account - Withdrawal");
        else
            actionBar.setTitle("Savings Account - Transaction");

        ButterKnife.inject(this, rootView);

        inflateUI();

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            //mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void inflateUI() {

        safeUIBlockingUtility.safelyBlockUI();

        tv_clientName.setText(clientName);
        tv_accountNumber.setText(savingsAccountNumber);
        //TODO Implement QuickContactBadge here

        et_transactionDate.setText(DateHelper.getCurrentDateAsString());

        inflatePaymentOptions();

    }

    public void inflatePaymentOptions() {

        API.savingsAccountService.getSavingsAccountTransactionTemplate(Integer.parseInt(savingsAccountNumber), new Callback<SavingsAccountTransactionTemplate>() {
            @Override
            public void success(SavingsAccountTransactionTemplate savingsAccountTransactionTemplate, Response response) {

                if(savingsAccountTransactionTemplate != null) {

                    List<String> listOfPaymentTypes = new ArrayList<String>();

                    //Currently this method assumes that Positions are Unique for each paymentType
                    //TODO Implement a Duplication check on positions and sort them and add into listOfPaymentTypes
                    paymentTypeOptionList = savingsAccountTransactionTemplate.getPaymentTypeOptions();
                    Iterator<PaymentTypeOption> paymentTypeOptionIterator = paymentTypeOptionList.iterator();
                    while(paymentTypeOptionIterator.hasNext())
                    {
                        PaymentTypeOption paymentTypeOption = paymentTypeOptionIterator.next();
                        listOfPaymentTypes.add(paymentTypeOption.getPosition(),paymentTypeOption.getName());
                        paymentTypeHashMap.put(paymentTypeOption.getName(),paymentTypeOption.getId());
                    }

                    ArrayAdapter<String> paymentTypeAdapter = new ArrayAdapter<String>(getActivity(),
                            android.R.layout.simple_spinner_item, listOfPaymentTypes);

                    paymentTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    sp_paymentType.setAdapter(paymentTypeAdapter);

                }

                safeUIBlockingUtility.safelyUnBlockUI();

            }

            @Override
            public void failure(RetrofitError retrofitError) {

                safeUIBlockingUtility.safelyUnBlockUI();

            }
        });



    }
    @OnClick(R.id.bt_reviewTransaction)
    public void onReviewTransactionButtonClicked() {

        String[] headers = {"Field", "Value"};
        String[][] data = {
                {"Transaction Date", et_transactionDate.getText().toString()},
                {"Payment Type", sp_paymentType.getSelectedItem().toString()},
                {"Amount", et_transactionAmount.getText().toString()}
        };

        System.out.println(FlipTable.of(headers, data));

        StringBuilder formReviewStringBuilder = new StringBuilder();

        for(int i=0;i<3;i++)
        {
            for(int j=0;j<2;j++)
            {
                formReviewStringBuilder.append(data[i][j]);
                if(j==0)
                {
                    formReviewStringBuilder.append(" : ");
                }
            }
            formReviewStringBuilder.append("\n");
        }



        AlertDialog confirmPaymentDialog = new AlertDialog.Builder(getActivity())
                .setTitle("Review Payment Details")
                .setMessage(formReviewStringBuilder.toString())
                .setPositiveButton("Process Transaction", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        processTransaction();
                    }
                })
                .setNegativeButton("Back", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .show();



    }

    public void processTransaction() {

        String dateString = et_transactionDate.getEditableText().toString().replace(" - ", " ");


        final SavingsAccountTransactionRequest savingsAccountTransactionRequest = new SavingsAccountTransactionRequest();
        savingsAccountTransactionRequest.setLocale("en");
        savingsAccountTransactionRequest.setDateFormat("dd MM yyyy");
        savingsAccountTransactionRequest.setTransactionDate(dateString);
        savingsAccountTransactionRequest.setTransactionAmount(et_transactionAmount.getEditableText().toString());
        savingsAccountTransactionRequest.setPaymentTypeId(String.valueOf(paymentTypeHashMap.get(sp_paymentType.getSelectedItem().toString())));

        String builtTransactionReuqestAsJson = new Gson().toJson(savingsAccountTransactionRequest);
        Log.i("Transaction Reuqest Body", builtTransactionReuqestAsJson);

        safeUIBlockingUtility.safelyBlockUI();

        API.savingsAccountService.processTransaction(Integer.parseInt(savingsAccountNumber),transactionType,
                savingsAccountTransactionRequest, new Callback<SavingsAccountTransactionResponse>() {
                    @Override
                    public void success(SavingsAccountTransactionResponse savingsAccountTransactionResponse, Response response) {

                        if(savingsAccountTransactionResponse != null)
                        {
                            if(transactionType.equals(Constants.SAVINGS_ACCOUNT_TRANSACTION_DEPOSIT))
                            {
                                Toast.makeText(getActivity(), "Deposit Successful, Transaction ID = " + savingsAccountTransactionResponse.getResourceId(),
                                        Toast.LENGTH_LONG).show();
                                getActivity().getSupportFragmentManager().popBackStackImmediate();

                            }else if(transactionType.equals(Constants.SAVINGS_ACCOUNT_TRANSACTION_WITHDRAWAL))
                            {
                                Toast.makeText(getActivity(), "Withdrawal Successful, Transaction ID = " + savingsAccountTransactionResponse.getResourceId(),
                                        Toast.LENGTH_LONG).show();
                                getActivity().getSupportFragmentManager().popBackStackImmediate();

                            }else
                            {
                                //Transaction Type Not Set - So user should never reach here
                                //TODO - Ask Vishwas about how to handle such events
                            }
                        }

                        safeUIBlockingUtility.safelyUnBlockUI();

                    }

                    @Override
                    public void failure(RetrofitError retrofitError) {
                        Toast.makeText(getActivity(),"Transaction Failed",Toast.LENGTH_SHORT).show();
                        safeUIBlockingUtility.safelyUnBlockUI();
                    }
                });
    }

    @OnClick(R.id.bt_cancelTransaction)
    public void onCancelTransactionButtonClicked() {
        getActivity().getSupportFragmentManager().popBackStackImmediate();
    }


    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

}
