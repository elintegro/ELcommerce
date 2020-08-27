package hilo

import io.hilo.Account
import io.hilo.AccountRole
import io.hilo.OAuthID
import io.hilo.Role
import io.hilo.common.RoleName

class Elintegro_ELcommerceController {
    def springSecurityService

    def userDetailsFromElintegro() {
        Account account = Account.findByUsername(params.userName)
        if (account == null) {
            Account newAccount = new Account()
            newAccount.username = params.userName
            newAccount.email = newAccount.username
            newAccount.name = "Rabindra Pangeni"
            def pw = new Random().toString()
            newAccount.password = springSecurityService.encodePassword(pw)
            newAccount.hasAdminRole = true
            newAccount.emailOptIn = false
            newAccount.save()
            OAuthID oAuthID = new OAuthID(accessToken: params.token, provider: "Elintegro",account: newAccount).save()
            newAccount.createAccountPermission()
            Role role = Role.findByAuthority(params.userRole)
            newAccount.createAccountRole(role)
            return newAccount
        }
        else{
            return account
        }
    }
    def authenticateWithToken(){
        def token = params.id
        OAuthID oAuthID = OAuthID.findByAccessToken(token)
        springSecurityService.reauthenticate(oAuthID.account.username)
        redirect(uri:"/hilo/admin")

    }
}
