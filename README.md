# E-Commerce App

Simple application pertaining the various requirements of an online commerce platform

## Getting Started

### Dependencies

* JAVA 21
* Docker
* An IDE that supports .env file injection into the run configuration (I, for one, used Intellij IDEA 2022 with EnvFile Plugin), without injection, the services will try to connect to their respective DB using hardcoded variable strings ${}
* Postman for testing

### Installing

* While it is present, the current docker-compose.yaml **HAS NOT BEEN TESTED, I DO NOT GUARANTEE THAT THE APP CAN BE FULLY DOCKERIZED AT THE MOMENT.**
* To instantiate the prerequisite containers, run docker-compose.infra.yaml
* If you chose to use Postman for testing, I have included in the folder "postman_stuff" the following:
  * The environment data for CUSTOMER (with customer user credentials)
  * The environment data for VISITOR (with visitor user credentials)
  * The collection with the endpoints of most concern
* For Keycloak, I have included the realm data in the folder "keycloak_stuff". The users that must be instantiated by hand are <u>customer</u> and <u>visitor</u>, passwords being their own names (make sure to propagate any changes in the Postman environments).
  * Make sure to assign the CUSTOMER realm role to customer and VISITOR to visitor as the security filter chains currently look for the **REALM ROLES** (that will be addressed later)

### Order of deployment of services

* Config - Discovery - Business Services - Gateway

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
* Add Swagger
* Fully Dockerize the app
* Code cleanup
