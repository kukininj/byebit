package com.example.byebit.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface EtherscanApiService {

    String BASE_URL = "https://api-sepolia.etherscan.io/api/";

    @GET("?module=account&action=txlist&sort=desc")
    Call<EtherscanApiResponse> getAccountTransactions(
            @Query("address") String address,
            @Query("startblock") long startBlock,
            @Query("endblock") long endBlock,
            @Query("page") int page,
            @Query("offset") int offset,
            @Query("apikey") String apikey // Key will be passed from Worker
    );

}
