package org.torusresearch.torusutilstest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.auth0.jwt.algorithms.Algorithm;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.torusresearch.fetchnodedetails.FetchNodeDetails;
import org.torusresearch.fetchnodedetails.types.NodeDetails;
import org.torusresearch.fetchnodedetails.types.TorusNetwork;
import org.torusresearch.torusutils.TorusUtils;
import org.torusresearch.torusutils.types.FinalKeyData;
import org.torusresearch.torusutils.types.FinalPubKeyData;
import org.torusresearch.torusutils.types.Metadata;
import org.torusresearch.torusutils.types.NodesData;
import org.torusresearch.torusutils.types.OAuthKeyData;
import org.torusresearch.torusutils.types.OAuthPubKeyData;
import org.torusresearch.torusutils.types.RetrieveSharesResponse;
import org.torusresearch.torusutils.types.SessionData;
import org.torusresearch.torusutils.types.TorusCtorOptions;
import org.torusresearch.torusutils.types.TorusException;
import org.torusresearch.torusutils.types.TorusPublicKey;
import org.torusresearch.torusutils.types.TypeOfUser;
import org.torusresearch.torusutils.types.VerifierArgs;
import org.torusresearch.torusutilstest.utils.JwtUtils;
import org.torusresearch.torusutilstest.utils.PemUtils;
import org.torusresearch.torusutilstest.utils.VerifyParams;
import org.web3j.crypto.Hash;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class TorusUtilsTest {

    static FetchNodeDetails fetchNodeDetails;

    static TorusUtils torusUtils;
    static Algorithm algorithmRs;

    static String TORUS_TEST_VERIFIER = "torus-test-health";
    static String TORUS_TEST_AGGREGATE_VERIFIER = "torus-test-health-aggregate";

    static String TORUS_TEST_EMAIL = "hello@tor.us";

    @BeforeAll
    static void setup() throws ExecutionException, InterruptedException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        System.out.println("Setup Starting");
        fetchNodeDetails = new FetchNodeDetails(TorusNetwork.TESTNET);
        TorusCtorOptions opts = new TorusCtorOptions("Custom");
        opts.setNetwork(TorusNetwork.TESTNET.toString());
        opts.setClientId("BG4pe3aBso5SjVbpotFQGnXVHgxhgOxnqnNBKyjfEJ3izFvIVWUaMIzoCrAfYag8O6t6a6AOvdLcS4JR2sQMjR4");
        torusUtils = new TorusUtils(opts);
        ECPrivateKey privateKey = (ECPrivateKey) PemUtils.readPrivateKeyFromFile("src/test/java/org/torusresearch/torusutilstest/keys/key.pem", "EC");
        ECPublicKey publicKey = (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(privateKey.getParams().getGenerator(), privateKey.getParams()));
        algorithmRs = Algorithm.ECDSA256(publicKey, privateKey);
    }

    @DisplayName("Gets Public Address")
    @Test
    public void shouldGetPublicAddress() throws ExecutionException, InterruptedException {
        VerifierArgs args = new VerifierArgs("google-lrc", TORUS_TEST_EMAIL, "extendedVerifierId");
        NodeDetails nodeDetails = fetchNodeDetails.getNodeDetails(args.getVerifier(), args.getVerifierId()).get();
        TorusPublicKey publicAddress = torusUtils.getPublicAddress(nodeDetails.getTorusNodeEndpoints(), nodeDetails.getTorusNodePub(), args).get();
        assertEquals("0x9bcBAde70546c0796c00323CD1b97fa0a425A506", publicAddress.getFinalPubKeyData().getEvmAddress());
        assertThat(publicAddress).isEqualToComparingFieldByFieldRecursively(new TorusPublicKey(
                new OAuthPubKeyData("0x9bcBAde70546c0796c00323CD1b97fa0a425A506",
                        "894f633b3734ddbf08867816bc55da60803c1e7c2a38b148b7fb2a84160a1bb5",
                        "1cf2ea7ac63ee1a34da2330413692ba8538bf7cd6512327343d918e0102a1438"),
                new FinalPubKeyData("0x9bcBAde70546c0796c00323CD1b97fa0a425A506",
                        "894f633b3734ddbf08867816bc55da60803c1e7c2a38b148b7fb2a84160a1bb5",
                        "1cf2ea7ac63ee1a34da2330413692ba8538bf7cd6512327343d918e0102a1438"),
                new Metadata(null, BigInteger.ZERO, TypeOfUser.v1, false),
                new NodesData(new ArrayList<>())
        ));
    }

    @DisplayName("Fetch User Type and Public Address")
    @Test
    public void shouldFetchUserTypeAndPublicAddress() throws ExecutionException, InterruptedException {
        VerifierArgs args = new VerifierArgs("google-lrc", TORUS_TEST_EMAIL, "");
        NodeDetails nodeDetails = fetchNodeDetails.getNodeDetails(args.getVerifier(), args.getVerifierId()).get();
        TorusPublicKey key = torusUtils.getUserTypeAndAddress(nodeDetails.getTorusNodeEndpoints(), nodeDetails.getTorusNodePub(), args).get();
        assertEquals("0x5b56E06009528Bffb1d6336575731ee3B63f6150", key.getFinalPubKeyData().getEvmAddress());
        assertEquals(TypeOfUser.v1, key.getMetadata().typeOfUser);

        String v2Verifier = "tkey-google-lrc";
        // 1/1 user
        String v2TestEmail = "somev2user@gmail.com";
        TorusPublicKey key2 = torusUtils.getUserTypeAndAddress(nodeDetails.getTorusNodeEndpoints(), nodeDetails.getTorusNodePub(), new VerifierArgs(v2Verifier, v2TestEmail, "")).get();
        assertEquals("0xE91200d82029603d73d6E307DbCbd9A7D0129d8D", key2.getFinalPubKeyData().getEvmAddress());
        assertEquals(TypeOfUser.v2, key2.getMetadata().getTypeOfUser());

        // 2/n user
        String v2nTestEmail = "caspertorus@gmail.com";
        TorusPublicKey key3 = torusUtils.getUserTypeAndAddress(nodeDetails.getTorusNodeEndpoints(), nodeDetails.getTorusNodePub(), new VerifierArgs(v2Verifier, v2nTestEmail, "")).get();
        assertEquals("0x1016DA7c47A04C76036637Ea02AcF1d29c64a456", key3.getFinalPubKeyData().getEvmAddress());
        assertEquals(TypeOfUser.v2, key3.getMetadata().getTypeOfUser());
    }

    @DisplayName("Key Assign test")
    @Test
    public void shouldKeyAssign() throws ExecutionException, InterruptedException {
        String email = JwtUtils.getRandomEmail();
        NodeDetails nodeDetails = fetchNodeDetails.getNodeDetails("google-lrc", email).get();
        TorusPublicKey publicAddress = torusUtils.getPublicAddress(nodeDetails.getTorusNodeEndpoints(), nodeDetails.getTorusNodePub(), new VerifierArgs("google-lrc", email, ""), false).get();
        System.out.println(email + " -> " + publicAddress.getFinalPubKeyData().getEvmAddress());
        assertNotNull(publicAddress.getFinalPubKeyData().getEvmAddress());
        assertNotEquals(publicAddress.getFinalPubKeyData().getEvmAddress(), "");
        assertNotNull(publicAddress.getoAuthPubKeyData().getEvmAddress());
        assertNotEquals(publicAddress.getoAuthPubKeyData().getEvmAddress(), "");
        assertEquals(publicAddress.getMetadata().getTypeOfUser(), TypeOfUser.v1);
        assertEquals(publicAddress.getMetadata().isUpgraded(), false);
    }

    @DisplayName("Login test")
    @Test
    public void shouldLogin() throws ExecutionException, InterruptedException, TorusException {
        NodeDetails nodeDetails = fetchNodeDetails.getNodeDetails(TORUS_TEST_VERIFIER, TORUS_TEST_EMAIL).get();
        RetrieveSharesResponse retrieveSharesResponse = torusUtils.retrieveShares(nodeDetails.getTorusNodeEndpoints(), nodeDetails.getTorusIndexes(), TORUS_TEST_VERIFIER, new HashMap<String, Object>() {{
            put("verifier_id", TORUS_TEST_EMAIL);
        }}, JwtUtils.generateIdToken(TORUS_TEST_EMAIL, algorithmRs)).get();
        assert (retrieveSharesResponse.getFinalKeyData().getPrivKey().equals("9b0fb017db14a0a25ed51f78a258713c8ae88b5e58a43acb70b22f9e2ee138e3"));
        //assertEquals("0xEfd7eDAebD0D99D1B7C8424b54835457dD005Dc4", retrieveSharesResponse.getFinalKeyData().getEvmAddress());
        assertThat(retrieveSharesResponse).isEqualToComparingFieldByFieldRecursively(new RetrieveSharesResponse(
                new FinalKeyData("0xF8d2d3cFC30949C1cb1499C9aAC8F9300535a8d6",
                        "49702976712193399986731725034276818613785907981142175961484729425380356961789",
                        "96786966479458068943089798058579864926773560415468505198145869253238919342057",
                        "9b0fb017db14a0a25ed51f78a258713c8ae88b5e58a43acb70b22f9e2ee138e3"),
                new OAuthKeyData("0xF8d2d3cFC30949C1cb1499C9aAC8F9300535a8d6",
                        "6de2e34d488dd6a6b596524075b032a5d5eb945bcc33923ab5b88fd4fd04b5fd",
                        "d5fb7b51b846e05362461357ec6e8ca075ea62507e2d5d7253b72b0b960927e9",
                        "9b0fb017db14a0a25ed51f78a258713c8ae88b5e58a43acb70b22f9e2ee138e3"),
                new SessionData(retrieveSharesResponse.sessionData.getSessionTokenData(), retrieveSharesResponse.sessionData.getSessionAuthKey()),
                new Metadata(null, BigInteger.ZERO, TypeOfUser.v1, false),
                new NodesData(retrieveSharesResponse.nodesData.nodeIndexes)
        ));
    }

    @DisplayName("Aggregate Login test")
    @Test
    public void shouldAggregateLogin() throws ExecutionException, InterruptedException, TorusException {
        String idToken = JwtUtils.generateIdToken(TORUS_TEST_EMAIL, algorithmRs);
        String hashedIdToken = Hash.sha3String(idToken).substring(2);
        NodeDetails nodeDetails = fetchNodeDetails.getNodeDetails(TORUS_TEST_AGGREGATE_VERIFIER, TORUS_TEST_EMAIL).get();
        RetrieveSharesResponse retrieveSharesResponse = torusUtils.retrieveShares(nodeDetails.getTorusNodeEndpoints(), nodeDetails.getTorusIndexes(), TORUS_TEST_AGGREGATE_VERIFIER, new HashMap<String, Object>() {{
            put("verify_params", new VerifyParams[]{new VerifyParams(idToken, TORUS_TEST_EMAIL)});
            put("sub_verifier_ids", new String[]{TORUS_TEST_VERIFIER});
            put("verifier_id", TORUS_TEST_EMAIL);
        }}, hashedIdToken).get();
        assertEquals("0x938a40E155d118BD31E439A9d92D67bd55317965", retrieveSharesResponse.getoAuthKeyData().getEvmAddress());
        assertThat(retrieveSharesResponse).isEqualToComparingFieldByFieldRecursively(new RetrieveSharesResponse(
                new FinalKeyData("0x938a40E155d118BD31E439A9d92D67bd55317965",
                        "12807676350687366924593653094908024592577690811576928555587654570837121768341",
                        "20253891874430456348096856713892060163029602605314794242419547596498838729190",
                        "3cbfa57d702327ec1af505adc88ad577804a1a7780bc013ed9e714c547fb5cb1"),
                new OAuthKeyData("0x938a40E155d118BD31E439A9d92D67bd55317965",
                        "1c50e34ef5b7afcf5b0c6501a6ae00ec3a09a321dd885c5073dd122e2a251b95",
                        "2cc74beb28f2c4a7c4034f80836d51b2781b36fefbeafb4eb1cd055bdf73b1e6",
                        "3cbfa57d702327ec1af505adc88ad577804a1a7780bc013ed9e714c547fb5cb1"),
                new SessionData(retrieveSharesResponse.sessionData.getSessionTokenData(), retrieveSharesResponse.sessionData.getSessionAuthKey()),
                new Metadata(null, BigInteger.ZERO, TypeOfUser.v1, false),
                new NodesData(retrieveSharesResponse.nodesData.nodeIndexes)
        ));
    }
}
