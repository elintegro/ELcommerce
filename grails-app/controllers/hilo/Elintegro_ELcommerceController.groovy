package hilo

import io.hilo.Account
import io.hilo.AccountRole
import io.hilo.OAuthID
import io.hilo.Role

class Elintegro_ELcommerceController {
    def springSecurityService

    def userDetailsFromElintegro() {
        Account account = Account.findByUsername(params.userName)
        if (account == null) {
            Account newUser = new Account()
            newUser.username = params.userName
            newUser.email = params.email
            newUser.name = params.firstName + params.lastName
            newUser.password = new Random()
            newUser.enabled = true
            newUser.save(flush: true)
            OAuthID oAuthID = new OAuthID(accessToken: params.token, provider: "Elintegro",user: newUser).save()
            AccountRole role = AccountRole.findByRole(params.userRole)
            Role.create(newUser,role)
            return newUser
        }
        else{
            return user
        }
    }
    def authenticateWithToken(){
        def token = params.id
        OAuthID oAuthID = OAuthID.findByAccessToken(token)
        springSecurityService.reauthenticate(oAuthID.user.username)
        redirect(uri:"/")

    }
}
