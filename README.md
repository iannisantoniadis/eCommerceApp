# E-Commerce App

Simple application that meets the various requirements of an online commerce platform

## Getting Started

### Dependencies

* JAVA 21
* Docker
* An IDE that supports .env file injection into the run configuration (I, for one, used IntelliJ IDEA 2022 with EnvFile Plugin), without injection, the services will try to connect to their respective DB using hardcoded variable strings ${}
* Postman for testing

### Installing

* To instantiate the prerequisite containers, run docker-compose.infra.yaml
* If you chose to use Postman for testing, I have included in the folder "postman_stuff" the following:
  * The environment data for CUSTOMER (with customer user credentials)
  * The environment data for VISITOR (with visitor user credentials)
  * The collection with the endpoints of most concern
* For Keycloak, I have included the realm data in the folder "keycloak_stuff". The users that must be instantiated by hand are <u>customer</u> and <u>visitor</u>, passwords being their own names (make sure to propagate any changes in the Postman environments).
  * Make sure to assign the CUSTOMER realm role to customer and VISITOR to visitor as the security filter chains currently look for the **REALM ROLES** (that will be addressed later)
* **FOR FULLY DOCKERIZED DEPLOY**:
  * Make sure to add in the windows hosts file: 127.0.0.1 keycloak
  * After deploying and restoring the keycloak realm, change the front end url in the realm settings from localhost:8080 to keycloak:8080, otherwise keycloak will redirect you to localhost, which is a concern because the cookie is emitted by keycloak 
  * **TLDR:** 
    * C:\Windows\System32\drivers\etc - In hosts add 127.0.0.1 keycloak
    * Keycloak UI - realm settings (of micro-services)  - front end url: **http://localhost:8080 → http://keycloak:8080**

### Order of deployment of services

* Config - Discovery - Payment - Other Business Services - Gateway

### Request Body examples
* post_order:
```
{
    "reference": "MS-20231201",
    "paymentMethod": "PAYPAL",
    "customerId": "6976768e6c275b76d8d7e78e",
    "products":[ 
        {
            "productId": 2,
            "quantity": 2
        },
        {
            "productId": 1,
            "quantity": 2
        }
    ]
}
```
* post_customer:
```
{
    "firstname": "Steve",
    "lastname": "Vai",
    "email": "stevevai@test.com",
    "address": {
        "street": "Street name",
        "houseNumber": "123",
        "zipCode": "50001"
    }
}
```

## TBA

* Make security filter chains extract client roles instead of realm roles
* ~~Add Swagger~~
* ~~Fully Dockerize the app~~
* Complete the Saga pattern via Transactional Outboxing on producer side and Idempotence on both sides
* Code cleanup
