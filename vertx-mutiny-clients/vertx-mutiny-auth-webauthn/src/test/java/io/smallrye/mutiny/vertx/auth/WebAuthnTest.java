package io.smallrye.mutiny.vertx.auth;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.webauthn.Authenticator;
import io.vertx.ext.auth.webauthn.RelyingParty;
import io.vertx.ext.auth.webauthn.WebAuthnOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.auth.User;
import io.vertx.mutiny.ext.auth.webauthn.WebAuthn;

public class WebAuthnTest {

    private Vertx vertx;
    private final DummyStore database = new DummyStore();

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
    }

    @After
    public void tearDown() {
        database.clear();
        vertx.closeAndAwait();
    }

    @Test(timeout = 1000)
    @Ignore("Invalid root certificate")
    public void testFIDORegister() {
        WebAuthn webAuthN = WebAuthn.create(
                vertx,
                new WebAuthnOptions().setRelyingParty(new RelyingParty().setName("FIDO Examples Corporation")))
                .authenticatorFetcher(database::fetch)
                .authenticatorUpdater(database::store);

        database.add(
                new Authenticator()
                        .setCredID("vp6cvoSgvTWSyFpnmdpm1dwiuREvsm-Kqw0Jt0Y0PQfjHsEhKE82KompUXqEt5yQIQl9ZKj6L1-700LGaVUMoQ")
                        .setPublicKey(
                                "pQECAyYgASFYINE091XO4J5juKbQMyeu9X2oZYFvAq6oIgp_3z1hXhG3Ilgg_cCyBgk8U8Zm3umoX2ELm6Is1k-2PLtwWmCkmul07cQ")
                        .setCounter(0));

        final JsonObject webauthn = new JsonObject(
                "{\"getClientExtensionResults\":{},\"rawId\":\"vp6cvoSgvTWSyFpnmdpm1dwiuREvsm-Kqw0Jt0Y0PQfjHsEhKE82KompUXqEt5yQIQl9ZKj6L1-700LGaVUMoQ\",\"response\":{\"attestationObject\":\"o2NmbXRoZmlkby11MmZnYXR0U3RtdKJjc2lnWEcwRQIhAOOPecQ34VN0QW-cmj-Sft9aCahqgTlFQzbQH1LpEgrTAiBWW6KoqlKbLMtGd1Y_VcQML8eugYZcrmSSCS0of2T-M2N4NWOBWQIyMIICLjCCARigAwIBAgIECmML_zALBgkqhkiG9w0BAQswLjEsMCoGA1UEAxMjWXViaWNvIFUyRiBSb290IENBIFNlcmlhbCA0NTcyMDA2MzEwIBcNMTQwODAxMDAwMDAwWhgPMjA1MDA5MDQwMDAwMDBaMCkxJzAlBgNVBAMMHll1YmljbyBVMkYgRUUgU2VyaWFsIDE3NDI2MzI5NTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABKQjZF26iyPtbNnl5IuTKs_fRWTHVzHxz1IHRRBrSbqWD60PCqUJPe4zkIRFqBa4NnzdhVcS80nlZuY3ANQm0J-jJjAkMCIGCSsGAQQBgsQKAgQVMS4zLjYuMS40LjEuNDE0ODIuMS4yMAsGCSqGSIb3DQEBCwOCAQEAZTmwMqHPxEjSB64Umwq2tGDKplAcEzrwmg6kgS8KPkJKXKSu9T1H6XBM9-LAE9cN48oUirFFmDIlTbZRXU2Vm2qO9OdrSVFY-qdbF9oti8CKAmPHuJZSW6ii7qNE59dHKUaP4lDYpnhRDqttWSUalh2LPDJQUpO9bsJPkgNZAhBUQMYZXL_MQZLRYkX-ld7llTNOX5u7n_4Y5EMr-lqOyVVC9lQ6JP6xoa9q6Zp9-Y9ZmLCecrrcuH6-pLDgAzPcc8qxhC2OR1B0ZSpI9RBgcT0KqnVE0tq1KEDeokPqF3MgmDRkJ--_a2pV0wAYfPC3tC57BtBdH_UXEB8xZVFhtGhhdXRoRGF0YVjESZYN5YgOjGh0NBcPZHZgW4_krrmihjLHmVzzuoMdl2NBAAAAAAAAAAAAAAAAAAAAAAAAAAAAQL6enL6EoL01kshaZ5naZtXcIrkRL7JviqsNCbdGND0H4x7BIShPNiqJqVF6hLeckCEJfWSo-i9fu9NCxmlVDKGlAQIDJiABIVgg0TT3Vc7gnmO4ptAzJ671fahlgW8CrqgiCn_fPWFeEbciWCD9wLIGCTxTxmbe6ahfYQuboizWT7Y8u3BaYKSa6XTtxA\",\"clientDataJSON\":\"eyJjaGFsbGVuZ2UiOiJQZXlodVVYaVQzeG55V1pqZWNaU1NxaFVTdUttYmZPV0dGREN0OGZDUXYwIiwiY2xpZW50RXh0ZW5zaW9ucyI6e30sImhhc2hBbGdvcml0aG0iOiJTSEEtMjU2Iiwib3JpZ2luIjoiaHR0cDovL2xvY2FsaG9zdDozMDAwIiwidHlwZSI6IndlYmF1dGhuLmNyZWF0ZSJ9\"},\"id\":\"vp6cvoSgvTWSyFpnmdpm1dwiuREvsm-Kqw0Jt0Y0PQfjHsEhKE82KompUXqEt5yQIQl9ZKj6L1-700LGaVUMoQ\",\"type\":\"public-unwrap\"}");

        User user = webAuthN.authenticate(
                new JsonObject()
                        .put("webauthn", webauthn)
                        .put("challenge", "PeyhuUXiT3xnyWZjecZSSqhUSuKmbfOWGFDCt8fCQv0")
                        .put("username", "paulo")
                        .put("origin", "http://localhost:3000"))
                .await().indefinitely();

        assertNotNull(user);
    }

    @Test(timeout = 1000)
    @Ignore("Invalid root certificate")
    public void testFIDOLogin() {
        WebAuthn webAuthN = WebAuthn.create(
                vertx,
                new WebAuthnOptions().setRelyingParty(new RelyingParty().setName("FIDO Examples Corporation"))
                        .setRequireResidentKey(true))
                .authenticatorFetcher(database::fetch)
                .authenticatorUpdater(database::store);

        database.add(
                new Authenticator()
                        .setCredID("-r1iW_eHUyIpU93f77odIrdUlNVfYzN-JPCTWGtdn-1wxdLxhlS9NmzLNbYsQ7XVZlGSWbh_63E5oFHcNh4JNw")
                        .setPublicKey(
                                "pQECAyYgASFYIB4QBsdBFyVm79aQFrgdhAFsV0bD0-UfzsRRihvSU8bnIlggdBaaNC3nGWGcZd1msfoD0vMt0Ydg9InOFKkz6PKUEf8")
                        .setCounter(0));

        final JsonObject webauthn = new JsonObject(
                "{\"getClientExtensionResults\":{},\"rawId\":\"-r1iW_eHUyIpU93f77odIrdUlNVfYzN-JPCTWGtdn-1wxdLxhlS9NmzLNbYsQ7XVZlGSWbh_63E5oFHcNh4JNw\",\"response\":{\"authenticatorData\":\"SZYN5YgOjGh0NBcPZHZgW4_krrmihjLHmVzzuoMdl2MBAAAAFA\",\"signature\":\"MEUCIA3bv92hSE3wNz1CNGIinx27YLJgucNnBwqjV7qWqHqiAiEAjBsxBaK2nEfCilGSZ3yzoHVJilwkhOOkwZAJ52xp-h8\",\"userHandle\":\"null\",\"clientDataJSON\":\"eyJjaGFsbGVuZ2UiOiI2b2pkb19LS0c0a1hvWjVKRF9BbHY2Q2hyVXRPT3o3dXFlaWlvRmxCc3pvIiwiY2xpZW50RXh0ZW5zaW9ucyI6e30sImhhc2hBbGdvcml0aG0iOiJTSEEtMjU2Iiwib3JpZ2luIjoiaHR0cDovL2xvY2FsaG9zdDozMDAwIiwidHlwZSI6IndlYmF1dGhuLmdldCJ9\"},\"id\":\"-r1iW_eHUyIpU93f77odIrdUlNVfYzN-JPCTWGtdn-1wxdLxhlS9NmzLNbYsQ7XVZlGSWbh_63E5oFHcNh4JNw\",\"type\":\"public-unwrap\"}");

        User user = webAuthN.authenticate(
                new JsonObject()
                        .put("webauthn", webauthn)
                        .put("origin", "http://localhost:3000")
                        .put("challenge", "6ojdo_KKG4kXoZ5JD_Alv6ChrUtOOz7uqeiioFlBszo"))
                .await().indefinitely();

        assertNotNull(user);
    }

}
