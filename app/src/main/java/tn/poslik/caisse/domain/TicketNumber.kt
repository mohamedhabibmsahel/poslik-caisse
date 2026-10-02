package tn.poslik.caisse.domain

/**
 * Numéro de ticket = code de caisse + compteur local de la caisse, ex. « C03-000124 ».
 *
 * Le code de caisse est attribué une seule fois par Firebase (transaction) ; le compteur
 * est incrémenté dans la même transaction SQLite que l'enregistrement de la vente.
 * Deux caisses ne peuvent donc jamais produire le même numéro, même hors ligne.
 */
object TicketNumber {
    fun registerCode(registerNumber: Long): String = "C%02d".format(registerNumber)

    fun format(registerCode: String, seq: Long): String = "%s-%06d".format(registerCode, seq)
}
