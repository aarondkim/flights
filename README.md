# Flight App

A prototype management service connected to a live database in Azure with a database design consisting of airline flights (data sourced from Bureau of Transportation Statistics), their customers, and their reservations. This prototype uses a command-line interface supporting the functionalities below:

UI:
```
*** Please enter one of the following commands ***
> create <username> <password> <initial amount>
> login <username> <password>
> search <origin city> <destination city> <direct> <day> <num itineraries>
> book <itinerary id>
> pay <reservation id>
> reservations
> quit
```

Supports searches for direct and indirect (one-stop) flight itineraries. Example itinerary searches:
Below is an example of a single direct itinerary from Seattle to Boston:

<img width="533" alt="Screen Shot 2025-04-09 at 2 58 41 PM" src="https://github.com/user-attachments/assets/61098fd5-73ca-4de5-962d-2fc683e75d45" />

Below is an example of an indirect itinerary from Seattle to Boston.

<img width="529" alt="Screen Shot 2025-04-09 at 2 59 19 PM" src="https://github.com/user-attachments/assets/04de4506-f464-42e1-9ecc-9e4b40dda976" />

