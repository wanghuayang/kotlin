package test

sealed class Base

sealed class Intermediate : Base()

class Impl : Intermediate()
