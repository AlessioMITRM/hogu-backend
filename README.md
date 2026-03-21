# Linee guida per l'avvio

## Profilo di sviluppo
- Per avviare l’applicazione in ambiente di sviluppo, utilizzare il profilo `dev`.  
- In Eclipse, inserire nei **Program Arguments** della configurazione Spring:
--spring.profiles.active=dev
- Per accesso al profilo client
  "email": "customer@gmail.com",
  "password": "123456",
- Per testare api di registrazione forntore
curl -X POST "http://localhost:8080/api/auth/provider-register" \
  -H "Content-Type: multipart/form-data" \
  -F "name=Mario Rossi" \
  -F "email=mario.rossi@example.com" \
  -F "password=123456" \
  -F "serviceType=RESTAURANT" \
  -F "documents[0].filename=documento1.pdf" \
  -F "documents[0].approved=true" \
  -F "documents[0].file=@/percorso/al/file/documento1.pdf"

## Profilo di collaudo/sviluppo
- Per avviare l'applicazione in ambiente di collaudo/sviluppo, utilizzare il profilo `stag`.
- In Eclipse, inserire nei **Program Arguments** della configurazione Spring:
- spring.profiles.active=stag
- Per fare accesso con Jwt e cuaa per il momento utilizzare:

docker compose -f docker-compose.dev.yml up -d

Da lanciare per sviluppo tramite telegram python telegram_bot.py

# Processi e flussi

## Flusso pagamenti Stripe
	Prenotazione backend POST /api/services/{servizio}/bookings
	Frontend → inserisce carta → genera paymentMethodId → lo manda al backend
	Backend → conferma pagamento con Stripe API
	
## Flusso pagamenti Paypal
    Prenotazione backend POST /api/services/{servizio}/bookings
	Cliente Frontend
	   - Clicca "Paga con PayPal" (button)
	
	Frontend → Backend (/paypal/create)
	   Request: {
	       bookingId: 123,
	       amount: 50.0,
	       currency: "EUR",
	       description: "Prenotazione NCC",
	       customerEmail: "utente@example.com"
	   }
	
	Backend → PayPal (Create Payment API)
	   Request: {
	       intent: "sale",
	       payer: { payment_method: "paypal" },
	       transactions: [
	           { amount: { total: 50.0, currency: "EUR" }, description: "Prenotazione NCC" }
	       ],
	       redirect_urls: {
	           return_url: "https://tuodominio.com/paypal/return",
	           cancel_url: "https://tuodominio.com/paypal/cancel"
	       }
	   }
	
	PayPal → Backend (Response)
	   Response: {
	       id: "PAYID-123456789",
	       links: [
	           { rel: "approval_url", href: "https://www.paypal.com/checkoutnow?token=PAYID-123456789" }
	       ]
	   }
	
	Backend → Frontend (Response)
	   Response: {
	       approvalUrl: "https://www.paypal.com/checkoutnow?token=PAYID-123456789"
	   }
	
	Frontend
	   - Redirect utente a PayPal → https://www.paypal.com/checkoutnow?token=PAYID-123456789
	
	Utente → PayPal
	   - Login + autorizza pagamento
	   - PayPal genera:
	       paymentId = "PAYID-123456789"
	       payerId   = "PAYERID-ABCDEF"
	
	PayPal → Frontend
	   - Redirect a frontend: https://tuodominio.com/paypal/return?paymentId=PAYID-123456789&PayerID=PAYERID-ABCDEF
	
	Frontend → Backend (/paypal/execute)
	   Request: {
	       paymentId: "PAYID-123456789",
	       payerId: "PAYERID-ABCDEF",
	       bookingId: 123
	   }
	
	Backend → PayPal (Execute Payment)
	   Request: {
	       paymentId: "PAYID-123456789",
	       payerId: "PAYERID-ABCDEF"
	   }
	
	PayPal → Backend (Response)
	   Response: {
	       id: "PAYID-123456789",
	       state: "approved",
	       transactions: [
	           { amount: { total: "50.00", currency: "EUR" } }
	       ]
	   }
	
	Backend
	   - Aggiorna DB: Payment.status = COMPLETED
	   - Aggiorna Booking.status = DEPOSIT_PAID
	   - Restituisce al frontend:
	       PaymentResponseDto {
	           paymentStatus: COMPLETED,
	           paymentIdIntent: "PAYID-123456789",
	           amount: 50.0,
	           currency: "EUR"
	       }
	
	Frontend
	   - Mostra conferma pagamento all’utente
	   
### Flusso pagamento comune:
	Autorizzazione (Hold/Pre-authorization):
	
	Stripe: puoi creare un PaymentIntent con capture_method='manual'. Questo blocca l’importo sulla carta dell’utente    senza addebitare subito.
	
	PayPal: puoi usare la funzione di authorization invece di sale. Blocca i fondi ma non li cattura fino all’approvazione.

### Todo da analizzare
	- da capire come comportarsi per fornitore che vuole avere piu ruoli servizio (proposta pannello di postaccesso per 	la 	selezioni del servizio che si vuole loggare)

### Implementazioni future
	- Programmazione con timer per tradurre le descrizioni dei servizi non tradotte dal cliente con IA
	- Pagamenti crypto

### Infrastruttura sistemi
	Dove lo creerai
	Piattaforma: account su DigitalOcean, sezione “Droplets” nel pannello web.​

	Autoscaling: sempre nel pannello, tab “Autoscale Pools”, dove crei il pool che scala automaticamente le VM in base a CPU/RAM.​

	Cosa ti servirà
	Un Droplet base con la tua app (Java Spring + React) funzionante e un’immagine/snapshot da usare come “template” per le istanze del pool.​

	Un database PostgreSQL separato: o Managed Database di DigitalOcean (scelta più semplice) o un Droplet dedicato solo per il DB.​

	Un Load Balancer DigitalOcean che gira davanti al pool, così tutto il traffico entra da un unico endpoint e viene distribuito alle istanze.​

	Cosa dovrai configurare
	Pool di autoscaling: nel wizard imposti

	minimo e massimo numero di Droplet (es. min 1, max 3–5),

	metrica da monitorare (CPU e/o RAM) e soglia di utilizzo (es. scala se CPU > 60–70% per X minuti),

	cooldown, cioè quanto tempo aspettare tra uno scale‑up/down e l’altro.​

	Config del Droplet nel pool: scegli regione (es. datacenter europeo), tag, immagine da usare e chiavi SSH.​

	In pratica, per il tuo caso
	Creerai tutto su DigitalOcean:

	1 Droplet “app template” per Spring + React.

	1 Managed PostgreSQL (o Droplet DB).

	1 Load Balancer + 1 Autoscale Pool che usa il template per creare/distruggere istanze.​

	Una volta fatto, i picchi sporadici (100k–500k visite al mese) verranno gestiti dal pool che alza e abbassa il numero di Droplet in automatico, così paghi di più solo nei giorni “caldi”.


### Test automatici
	UTENTE PER POTER FARE PRENOTAZIONI: customer@gmail.com pwd: 123456
	FORNITORE NCC: ncc@gmail.com pwd: 123456
	ADMIN: admin@gmail.com pwd: 123456
	FORNITORE RISTORANTE: resturant@gmail.com pwd: 123456
	FORNITORE BAGAGLI: luggage@gmail.com pwd: 123456
	FORNITORE CLUB: club@gmail.com pwd: 123456
	FORNITORE BNB: bnb@gmail.com pwd: 123456

	Prenotazione con stripe:
			| Carta            | Tipo    | Numero              | Scadenza         | CVC                 |
			| ---------------- | ------- | ------------------- | ---------------- | ------------------- |
			| Visa             | Credito | 4242 4242 4242 4242 | Qualsiasi futura | Qualsiasi           |
			| Mastercard       | Credito | 5555 5555 5555 4444 | Qualsiasi futura | Qualsiasi           |
			| American Express | Credito | 3782 822463 10005   | Qualsiasi futura | Qualsiasi (4 cifre) |
			| Discover         | Credito | 6011 1111 1111 1117 | Qualsiasi futura | Qualsiasi           |
			| Diners Club      | Credito | 3056 930902 5904    | Qualsiasi futura | Qualsiasi           |
			| JCB              | Credito | 3566 1111 1111 1113 | Qualsiasi futura | Qualsiasi           |

		Carte di test per scenari specifici:
			Pagamento rifiutato: 4000 0000 0000 9995 → simula un pagamento rifiutato.
			Pagamento insufficiente fondi: 4000 0000 0000 9999
			Pagamento segnalato come frode: 4100 0000 0000 0019
			Autenticazione richiesta (3D Secure): 4000 0025 0000 3155