#!/usr/bin/env python

import logging
import sys
import os
import inspect

from smac.smac_cli import SMACCLI

if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    smac = SMACCLI()
    smac.main_cli()
