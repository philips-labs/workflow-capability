# Management Dashboard  

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 13.2.4.
Management Dashboard is a fully responsive reporting dashboard based on Bootstrap, AdminLTE, and Angular frameworks.
Highly customizable and easy to use. Fits many screen resolutions from small mobile devices to large desktops.


## TOC
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installing & Local Development](#installing--local-development)
- [Files/Folder Structure](#filesfolders-structure)
- [Deployment](#deployment)
- [Built With](#built-with)
- [Changelog](#changelog)
- [Authors](#authors)


## Getting Started
In order to run **Dashboard** on your local machine all what you need to do is 
to have the prerequisites stated below installed on your machine and follow the installation steps down below.

#### Prerequisites
- Node.js
- Git

#### Installing & Local Development
Start by typing the following commands in your terminal in order to get **Dashboard** full package on 
your machine and starting a local development server with live reload feature.

```
> cd management_dashboard
> npm install
> npm run start
```


## Files/Folders Structure
Here is a brief explanation of the template folder structure and some of its main files usage:

```
└── src                         # Contains all template source files.
│   └── app                     # Contains all the angular web components
│   │   └── components          # Contains all JavaScript files.
│   │   │   └── aside           # The side bar component
│   │   │   └── content         # The main reporting page component
│   │   │   └── footer          # The footer component
│   │   │   └── navbar          # The navigaiton bar component
│   │   └── app.component.html  # the root app components where all the components are placed
│   │   
│   │── assets                  # Contains the non-code css and font files.
│   │   └── webfonts            # Contains icon fonts.
│   │   └── CSS                 # Contains all CSS files
│   │   
│   │── environments            # Contains local or production environment variables
│   │
│   └── *.html                  # Index.HTML page
│   └── *.ts                    # Angular TS module
│   └── *.css                   # Css Styles used after building the project.
|
└── .gitignore                  # Ignored files in Git.
└── .angular                    # Contains angular libraries.
└── package.json                # Package metadata.
└── README.md                   # Manual file.
└── Angular.json                # Angular main config file.
```

## Deployment
In order to deploy the app, use the `npm run build` command. It is used to generate the final result of compiling src files into build folder.

## Built With
- [Angular](https://angular.io/)
- [NodeJS](https://nodejs.org/en/)
- [Bootstrap](http://getbootstrap.com/)

## Changelog
#### V 1.0.0
Initial Release

## Authors
Software Technology Trainees 2021, Eindhoven University of Technology  
Moslem, Shabnam, Georgios, Lin, Hiba, Haftom, Pooyan, Ahmed, Pasha
