#!/usr/bin/env bash
# The purpose of this file is to make the docsite in a python virtualenv
# You can call it manually if running make manually, otherwise gradle will run it for you

SCRIPT_DIR=$(dirname $(readlink -f ${BASH_SOURCE[0]}))
export absolutevirtualenv="$SCRIPT_DIR/build/virtualenv"
export TEXMFHOME="$SCRIPT_DIR/build/texmf"

# Activate the virtualenv
if [ -d "$absolutevirtualenv/bin" ]
then
    # it's a Unix system
    source "$absolutevirtualenv/bin/activate"
else
    source "$absolutevirtualenv/Scripts/activate"
fi

echo "Generating HTML pages ..."
make SPHINXOPTS=-W html -C ${SCRIPT_DIR}

# TODO investigate making this work
#echo "Generating PDF document ..."
#make latexpdf
#echo "Moving PDF file from $(eval echo $SCRIPT_DIR/build/latex/corda-developer-site.pdf) to $(eval echo $SCRIPT_DIR/build/html/_static/corda-developer-site.pdf)"
#mv $SCRIPT_DIR/build/latex/sgxjvm-guide.pdf $PWD/build/html/_static/sgxjvm-guide.pdf
