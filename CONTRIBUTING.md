# How to contribute

We're really glad you're reading this, because we need more volunteer developers
to help with this project!

We can use all the help we can get on all our [GraphQL Java Kickstart](https://github.com/graphql-java-kickstart)
projects. This work ranges from adding new features, fixing bugs, and answering questions to writing documentation. 

## Answering questions and writing documentation

A lot of the questions asked on Spectrum or Github are caused by a lack of documentation.
We should strive from now on to answer questions by adding content to 
our [documentation](https://github.com/graphql-java-kickstart/documentation) and referring
them to the newly created content.

Continuous integration will make sure that the changes are automatically deployed to 
https://www.graphql-java-kickstart.com.

## Submitting changes

Please send a Pull Request with a clear list of what you've done using the
[Github flow](https://guides.github.com/introduction/flow/). We can always use more
test coverage, so we'd love to see that in the pull requests too. And make sure to
follow our coding conventions (below) and make sure all your commits are atomic 
(one feature per commit).

## Coding conventions

We use Google Style guides for our projects. See the 
[Java Style Guide](https://google.github.io/styleguide/javaguide.html) for a detailed
description. You can download the 
[IntelliJ Java Google Style](https://github.com/google/styleguide/blob/gh-pages/intellij-java-google-style.xml)
to import in these settings in IntelliJ.

These conventions are checked during the build phase. If the build fails because
the code is not using the correct style you can fix this easily by running a gradle task
```bash
./gradlew googleJavaFormat
```

### SonarLint

It would also be very helpful to install the SonarLint plugin in your IDE and fix any
relevant SonarLint issues before pushing a PR. We're aware that the current state
of the code raises a lot of SonarLint issues out of the box, but any help in reducing
that is appreciated. More importantly we don't increase that technical debt.
