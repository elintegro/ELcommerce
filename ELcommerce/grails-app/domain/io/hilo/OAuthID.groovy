package io.hilo

class OAuthID implements Serializable {

    String provider
    String accessToken

    static belongsTo = [account: Account]

    static constraints = {
        accessToken unique: true
    }

    static mapping = {
        provider    index: "identity_idx"
        accessToken index: "identity_idx"
    }
}
