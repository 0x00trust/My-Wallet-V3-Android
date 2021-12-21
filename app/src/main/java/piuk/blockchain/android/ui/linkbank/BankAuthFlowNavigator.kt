package piuk.blockchain.android.ui.linkbank

import com.blockchain.banking.BankPaymentApproval
import com.blockchain.nabu.models.data.YapilyAttributes
import com.blockchain.nabu.models.data.YapilyInstitution
import com.blockchain.nabu.models.data.YodleeAttributes
import info.blockchain.balance.FiatCurrency

interface BankAuthFlowNavigator {
    fun launchYodleeSplash(attributes: YodleeAttributes, bankId: String)
    fun launchYodleeWebview(attributes: YodleeAttributes, bankId: String)
    fun launchBankLinking(accountProviderId: String, accountId: String, bankId: String)
    fun retry()
    fun bankLinkingFinished(bankId: String, currency: FiatCurrency)
    fun bankAuthCancelled()
    fun launchYapilyBankSelection(attributes: YapilyAttributes)
    fun showTransferDetails()
    fun yapilyInstitutionSelected(institution: YapilyInstitution, entity: String)
    fun yapilyAgreementAccepted(institution: YapilyInstitution)
    fun yapilyApprovalAccepted(approvalDetails: BankPaymentApproval)
    fun yapilyAgreementCancelled(isFromApproval: Boolean)
}
